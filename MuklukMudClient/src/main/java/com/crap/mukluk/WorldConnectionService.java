package com.crap.mukluk;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

// Service that manages open connections to worlds
public class WorldConnectionService extends Service
{
	//private static final String TAG = "WorldConnectionService";
	
	public static final int STATUS_NOT_CONNECTED = 0;
	public static final int STATUS_CONNECTING = 1;
	public static final int STATUS_CONNECTED = 2;
	public static final int STATUS_DISCONNECTING = 3;
	
	@SuppressWarnings("unchecked")
    private static final Class[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
	@SuppressWarnings("unchecked")
    private static final Class[] mStopForegroundSignature = new Class[] {boolean.class};

	private WifiLock wifiLock = null;
	
	private NotificationManager mNM;
	private Notification notification;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	
	// Key = World's unique dbID, Value = connection to world
	private static HashMap<Integer, WorldConnection> allWorldConnections = new HashMap<Integer, WorldConnection>();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
	    mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		try
		{
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
		}
		catch (NoSuchMethodException e)
		{
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
	}
	
	@Override
	// start a TCP connection to the specified world, or refresh connections depending on extras
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);

        if (intent == null)  {
            refreshConnections();
        }
        else {
            Bundle extras = intent.getExtras();

            if (extras.containsKey("world")) {
                World world = (World) extras.getParcelable("world");

                WorldConnection worldConnection = new WorldConnection(world);
                allWorldConnections.put(world.dbID, worldConnection);
                worldConnection.tcpConnection.start();

                notification = new Notification(R.mipmap.ic_notification_boot, getString(R.string.service_started), System.currentTimeMillis());

                // Set intent to switch to current activity instead of starting a new one
                Intent switchToTask = new Intent(Intent.ACTION_MAIN, null, this, WorldListActivity.class);
                switchToTask.addCategory(Intent.CATEGORY_LAUNCHER);

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, switchToTask, 0);

                if (allWorldConnections.size() == 1)
                    notification.setLatestEventInfo(this, getString(R.string.service_title), String.format(getString(R.string.service_description_singular), allWorldConnections.size()), contentIntent);
                else
                    notification.setLatestEventInfo(this, getString(R.string.service_title), String.format(getString(R.string.service_description_plural), allWorldConnections.size()), contentIntent);

                startForeground(R.string.service_started, notification);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                if (prefs.getBoolean("keep_wifi_awake", false)) {
                    WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    wifiLock = wm.createWifiLock("mukluk");
                    wifiLock.acquire();
                }
            } else if (extras.containsKey("refresh")) {
                refreshConnections();
            }
        }
	}

	public void onDestroy()
	{
		super.onDestroy();
		
		if (wifiLock != null)
		{
			wifiLock.release();
			wifiLock = null;
		}

        stopForeground(true);
		
		HashSet<Integer> keys = new HashSet<Integer>();
		keys.addAll(allWorldConnections.keySet());
		
		for (int id : keys)
			closeConnectionToWorld(id);
	}
	
	// returns array of 2
	// 0 - world -> activity messages
	// 1 - activity -> world messages
	// returns null for requests for worlds that don't exist or worlds that are disconnecting or not connected
	public static ArrayList<BlockingQueue<WorldMessage>> getBindInformationForConnection(int worldID)
	{
		WorldConnection conn = allWorldConnections.get(worldID);
		
		if (conn != null)
		{
			ArrayList<BlockingQueue<WorldMessage>> inAndOut = new ArrayList<BlockingQueue<WorldMessage>>(2);
			
			inAndOut.add(conn.worldToActivityQueue);
			inAndOut.add(conn.activityToWorldQueue);
			
			return inAndOut;
		}
		else
		{
			return null;
		}
	}
	
	public static void closeConnectionToWorld(int worldID)
	{
		if (allWorldConnections.containsKey(worldID))
		{
			WorldConnection conn = allWorldConnections.get(worldID);
			
			allWorldConnections.remove(worldID);
			
			if (conn.tcpConnection.getStatus() == STATUS_CONNECTED)
			{
				conn.tcpConnection.forceDisconnect();
				allWorldConnections.remove(worldID);
			}
		}
	}
	
	public static Charset getWorldConnectionEncoding(int worldID)
	{
		WorldConnection conn = allWorldConnections.get(worldID);
		
		if (conn == null)
			return null;
		else
			return conn.tcpConnection.getEncoding();
	}
	
	public static int getWorldConnectionStatus(int worldID)
	{
		WorldConnection conn = allWorldConnections.get(worldID);
		
		if (conn == null)
			return STATUS_NOT_CONNECTED;
		else
			return conn.tcpConnection.getStatus();
	}
	
	public static String getWorldConnectionStatusDescription(Context context, int connectionStatus)
	{
		switch (connectionStatus)
		{
			case STATUS_NOT_CONNECTED :
				return context.getString(R.string.status_description_not_connected);
			case STATUS_CONNECTING :
				return context.getString(R.string.status_description_connecting);
			case STATUS_CONNECTED :
				return context.getString(R.string.status_description_connected);
			case STATUS_DISCONNECTING:
				return context.getString(R.string.status_description_disconnecting);
			default:
				return context.getString(R.string.status_description_unknown);
		}
	}
	
	public void refreshConnections()
	{
		ArrayList<Integer> removeThese = new ArrayList<Integer>();
		
		for (WorldConnection conn : allWorldConnections.values())
		{
			if (conn.tcpConnection.getStatus() != STATUS_CONNECTING && conn.tcpConnection.getStatus() != STATUS_CONNECTED)
				removeThese.add(conn.tcpConnection.getID());
		}
		
		for (int id : removeThese)
			allWorldConnections.remove(id);
		
		if (allWorldConnections.size() == 0)
			stopSelf();
		else
		{
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, WorldListActivity.class), 0);
			
			notification = new Notification(R.mipmap.ic_notification_boot, getString(R.string.service_started), System.currentTimeMillis());
			
			if (allWorldConnections.size() == 1)
				notification.setLatestEventInfo(this, getString(R.string.service_title), String.format(getString(R.string.service_description_singular), allWorldConnections.size()), contentIntent);
			else
				notification.setLatestEventInfo(this, getString(R.string.service_title), String.format(getString(R.string.service_description_plural), allWorldConnections.size()), contentIntent);
			
			mNM.notify(R.string.service_started, notification);
		}
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// Not used
		return null;
	}	
	
	// queues need to be accessed before the thread may have necessarily created them
	class WorldConnection
	{
		public BlockingQueue<WorldMessage> worldToActivityQueue;
		public BlockingQueue<WorldMessage> activityToWorldQueue;
		public TcpConnection tcpConnection;
		
		public WorldConnection(World world)
		{
			worldToActivityQueue = new BlockingQueue<WorldMessage>();
			activityToWorldQueue = new BlockingQueue<WorldMessage>();
			tcpConnection = new TcpConnection(world, worldToActivityQueue, activityToWorldQueue);
		}
	}
}
