package com.crap.mukluk;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public abstract class Utility
{
    private static final Pattern NON_ALPHA_PATTERN = Pattern.compile("[^A-Za-z\\d]");
    private static String[] _supportedCharsetNames = null;

    // Get an array with names of the supported charsets. Cache this so it only
    // has to be got once
    public static String[] getSupportedCharsetNames()
    {
        if (_supportedCharsetNames == null)
        {
            LinkedList<String> availableCharsets = new LinkedList<String>();

            for (Charset enc : Charset.availableCharsets().values())
            {
                availableCharsets.add(enc.name());
            }

            _supportedCharsetNames = new String[availableCharsets.size()];

            availableCharsets.toArray(_supportedCharsetNames);
        }

        return _supportedCharsetNames;
    }

    public static boolean isInternetConnectionAvailable(Context context)
    {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isAvailable() || mobile.isAvailable())
            return true;
        else
            return false;
    }

    public static String stripNonAlphanumericCharacters(String originalString)
    {
        Matcher m = NON_ALPHA_PATTERN.matcher(originalString);

        return m.replaceAll("");
    }
}
