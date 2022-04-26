package com.crap.mukluk;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

// An individual MUD, MOO, etc. and its settings
public class World implements Parcelable
{
    private static final String HOST_FORMAT = "(%1$s:%2$s)";

    public int dbID = -1;
    public String name;
    public String host;
    public int port = 0;
    public boolean loggingEnabled = false;
    public boolean ansiColorEnabled = false;
    public Charset encoding;
    public List<String> postLoginCommands = new ArrayList<String>();

    public World() {}

    public World(int id, String name, String host, int port, boolean loggingEnabled, boolean ansiColor,
                 String charsetName, List<String> postLoginCommands)
    {
        this.dbID = id;
        this.name = name;
        this.host = host;
        this.port = port;
        this.loggingEnabled = loggingEnabled;
        this.ansiColorEnabled = ansiColor;
        setCharset(charsetName);
        this.postLoginCommands.addAll(postLoginCommands);
    }

    public World(Parcel in)
    {
        dbID = in.readInt();
        name = in.readString();
        host = in.readString();
        port = in.readInt();
        loggingEnabled = in.readInt() > 0 ? true : false;
        ansiColorEnabled = in.readInt() > 0 ? true : false; // originally
        // auto-connect
        setCharset(in.readString());
        in.readStringList(postLoginCommands);
    }

    // Get the contentValues for this world. Doesn't include post-login
    // commands.
    public ContentValues getContentValues()
    {
        ContentValues vals = new ContentValues(6);

        vals.put("name", name);
        vals.put("host", host);
        vals.put("port", port);
        vals.put("loggingEnabled", loggingEnabled);
        vals.put("ansiColorEnabled", ansiColorEnabled);
        vals.put("encoding", encoding.name());

        return vals;
    }

    public static final Parcelable.Creator<World> CREATOR = new Parcelable.Creator<World>()
    {
        public World createFromParcel(Parcel in)
        {
            return new World(in);
        }

        public World[] newArray(int size)
        {
            return new World[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    public String getCommandHistoryCacheFileName()
    {
        return "CommandHistory_" + dbID;
    }

    public String getConsoleCacheFileName()
    {
        return "ConsoleHistory_" + dbID;
    }

    public String getConsoleStyleCacheJsonFileName()
    {
        return "ConsoleStyleHistoryJson_" + dbID;
    }

    public String getConsoleStyleCacheJacksonJsonFileName()
    {
        return "ConsoleStyleHistoryJson_v2_" + dbID;
    }

    public String getURLCacheFileName()
    {
        return "URLHistory_" + dbID;
    }

    public String getHostAsString()
    {
        return String.format(HOST_FORMAT, host, port);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(dbID);
        dest.writeString(name);
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeInt(loggingEnabled ? 1 : 0);
        dest.writeInt(ansiColorEnabled ? 1 : 0);
        dest.writeString(encoding.name());
        dest.writeStringList(postLoginCommands);
    }

    public void setCharset(String charsetName)
    {
        if (charsetName == null || charsetName.length() == 0)
        {
            this.encoding = Charset.forName("UTF-8");
        }
        else
        {
            try
            {
                this.encoding = Charset.forName(charsetName);
            }
            catch (IllegalCharsetNameException ex)
            {
                this.encoding = Charset.forName("UTF-8");
            }
            catch (UnsupportedCharsetException ex)
            {
                this.encoding = Charset.forName("UTF-8");
            }
        }
    }
}
