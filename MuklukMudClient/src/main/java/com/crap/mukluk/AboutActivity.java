package com.crap.mukluk;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        
        TextView version = (TextView) findViewById(R.id.text_version);
        PackageManager pm = getPackageManager();
        PackageInfo pInfo;
        
        try
        {
	        pInfo = pm.getPackageInfo("com.crap.mukluk", 0);
	        version.setText(pInfo.versionName);
        }
        catch (NameNotFoundException ex)
        {
        	version.setText("Unknown Version");
        }
    }
}
