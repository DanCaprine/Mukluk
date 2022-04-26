package com.crap.mukluk;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

// adapter that converts World[] to a view
public class WorldListAdapter extends BaseAdapter
{
	private Context context;
	private World[] worlds;
	private LayoutInflater inflater;
	
	public WorldListAdapter(Context context, World[] worlds)
	{
		this.context = context;
		this.worlds = worlds;
		inflater = LayoutInflater.from(context);
	}
	
	public int getConnectedWorlds()
	{
		int numConnected = 0;
		
		for (World w : worlds)
		{
			if (WorldConnectionService.getWorldConnectionStatus(w.dbID) == WorldConnectionService.STATUS_CONNECTED)
				numConnected++;
		}
				
		return numConnected;
	}
	
	@Override
	public int getCount()
	{
		return worlds.length;
	}

	@Override
	public Object getItem(int position)
	{
		return worlds[position];
	}

	@Override
	public long getItemId(int position)
	{
		return worlds[position].dbID;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		World world = worlds[position];
		RelativeLayout row;

		if (convertView == null || !(convertView instanceof RelativeLayout))
			row = (RelativeLayout) inflater.inflate(R.layout.world_list_row, null);
		else
			row = (RelativeLayout) convertView;
		
		TextView txtName = (TextView) row.findViewById(R.id.row_world_name);
		txtName.setText(world.name.trim().length() == 0 ? "[Unnamed World]" : world.name);
		
		TextView txtHost = (TextView) row.findViewById(R.id.row_world_host);
		txtHost.setText(world.host.trim().length() == 0 ? "[world address not set]" : world.host + " : " + world.port);
		
		TextView txtStatus = (TextView) row.findViewById(R.id.row_world_status);
		
		txtStatus.setText(WorldConnectionService.getWorldConnectionStatusDescription(context, WorldConnectionService.getWorldConnectionStatus(world.dbID)));
		
		switch (WorldConnectionService.getWorldConnectionStatus(world.dbID))
		{
			case (WorldConnectionService.STATUS_NOT_CONNECTED):
				txtStatus.setTextColor(Color.RED);
				break;
			case (WorldConnectionService.STATUS_CONNECTING):
				txtStatus.setTextColor(Color.GREEN);
				break;
			case (WorldConnectionService.STATUS_CONNECTED):
				txtStatus.setTextColor(Color.GREEN);
				break;
			case (WorldConnectionService.STATUS_DISCONNECTING):
				txtStatus.setTextColor(Color.RED);
				break;
		}
		
		return row;
	}

}
