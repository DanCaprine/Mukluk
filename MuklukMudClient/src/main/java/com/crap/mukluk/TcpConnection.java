package com.crap.mukluk;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

// An individual connection to a world
class TcpConnection extends Thread
{
	//private String tag;
	private int connectionStatus = WorldConnectionService.STATUS_NOT_CONNECTED;
	private Object statusLock = new Object();
	private World world;
	private Socket socket;
	private MessagesToWorldProcessor messagesToWorldProcessor;
	private BlockingQueue<WorldMessage> messagesFromWorldQueue;
	private BlockingQueue<WorldMessage> messagesToWorldQueue;

	public TcpConnection(World world, BlockingQueue<WorldMessage> messagesFromWorldQueue, BlockingQueue<WorldMessage> messagesToWorldQueue)
	{
		this.world = world;
		this.messagesFromWorldQueue = messagesFromWorldQueue;
		this.messagesToWorldQueue = messagesToWorldQueue;

		//tag = "TcpConnection " + world.getHostAsString();
	}
	
	public Charset getEncoding()
	{
		return world.encoding;
	}
	
	public int getID()
	{
		return world.dbID;
	}

	public int getStatus()
	{
		synchronized (statusLock)
		{
			return connectionStatus;
		}
	}

	public void setStatus(int newStatus)
	{
		synchronized (statusLock)
		{
			connectionStatus = newStatus;
		}
	}

	public void forceDisconnect()
	{
		setStatus(WorldConnectionService.STATUS_DISCONNECTING);
		
		if (socket != null)
		{
			// only way to interrupt Java socket waiting on read input
			try
			{
				// doing this first = android work-around for bug
				socket.shutdownInput();
			}
			catch (IOException ex) {}
			catch (NullPointerException ex) {}
			
			try
			{
				socket.close();
			}
			catch (IOException ex) {}
			catch (NullPointerException ex) {}
		}
	}
	
	@Override
	public void run()
	{
		InputStreamReader in;
		PrintWriter out;

		try
		{
			setStatus(WorldConnectionService.STATUS_CONNECTING);
			sendMessage(WorldMessage.MESSAGE_TYPE_STATUS_CHANGE,  WorldConnectionService.STATUS_CONNECTING, null);

			socket = new Socket(world.host, world.port);
			socket.setSoTimeout(120000); // 2 minutes
			in = new InputStreamReader(socket.getInputStream(), world.encoding);
			out = new PrintWriter(socket.getOutputStream(), true);

			setStatus( WorldConnectionService.STATUS_CONNECTED);
			sendMessage(WorldMessage.MESSAGE_TYPE_STATUS_CHANGE,  WorldConnectionService.STATUS_CONNECTED, null);
		}
		catch (IOException ex)
		{
			setStatus(WorldConnectionService.STATUS_NOT_CONNECTED);
			sendMessage(WorldMessage.MESSAGE_TYPE_ERROR, WorldConnectionService.STATUS_NOT_CONNECTED, "Unable to connect to world: " + ex.toString());
			sendMessage(WorldMessage.MESSAGE_TYPE_STATUS_CHANGE, WorldConnectionService.STATUS_NOT_CONNECTED, null);
			return;
		}
		
		messagesToWorldProcessor = new MessagesToWorldProcessor(out);
		messagesToWorldProcessor.start();

		// This is a hack to deal with the fact that not all worlds send a newline after each message
		// When the buffer is empty, wait 2 minutes between idle messages. When the buffer isn't, send what's left after a second of idleness
		StringBuilder builder = new StringBuilder();
		char c;
		
		while (getStatus() ==  WorldConnectionService.STATUS_CONNECTED)
		{	
			try
			{
				int iC = in.read();
				c = (char) iC;
				
				if (iC == -1)
				{
					setStatus(WorldConnectionService.STATUS_NOT_CONNECTED);
				}
				else if (c == '\n')
				{
					builder.append(c);
					sendMessage(WorldMessage.MESSAGE_TYPE_TEXT,  WorldConnectionService.STATUS_CONNECTED, builder);
					builder = new StringBuilder();
					socket.setSoTimeout(120000); // 2 minutes
				}
				else
				{				
					builder.append(c);
					socket.setSoTimeout(1000); // 1 second	
				}
			}
			catch (SocketTimeoutException ex)
			{
				// Send whatever's currently in the buffer
				
				if (builder.length() > 0)
				{
					sendMessage(WorldMessage.MESSAGE_TYPE_TEXT,  WorldConnectionService.STATUS_CONNECTED, builder);
					builder = new StringBuilder();
				}
			}
			catch (IOException ex)
			{				
				setStatus(WorldConnectionService.STATUS_NOT_CONNECTED);
			}
		}

		sendMessage(WorldMessage.MESSAGE_TYPE_STATUS_CHANGE, WorldConnectionService.STATUS_NOT_CONNECTED, null);
		messagesToWorldProcessor.stopProcessing = true;
		messagesToWorldProcessor.interrupt();
		messagesToWorldProcessor = null;
		
		try
		{
			in.close();
			in = null;
		}
		catch (Exception ex) {}
		
		try
		{
			out.close();
			out = null;
		}
		catch (Exception ex) {}

		try
		{
			socket.close();
			socket = null;
		}
		catch (Exception ex) {}
	}
	
	private void sendMessage(int type, int currentStatus, CharSequence text)
	{
		messagesFromWorldQueue.addMessage(new WorldMessage(type, currentStatus, text));
	}
	
	// Pass messages received in queue along to activity
	// to stop, set stopProcessing == false and then interrupt thread
	class MessagesToWorldProcessor extends Thread
	{
		private PrintWriter printWriter;
		
		public volatile boolean stopProcessing = false;
		
		public MessagesToWorldProcessor(PrintWriter printWriter)
		{
			this.printWriter = printWriter;
		}
		
		@Override
		public void run()
		{
			while (!stopProcessing)
			{
				WorldMessage wMsg = messagesToWorldQueue.getMessage();
				
				if (wMsg != null)
				{
					printWriter.println(wMsg.text);
					printWriter.flush();
				}
			}
		}
	}
}