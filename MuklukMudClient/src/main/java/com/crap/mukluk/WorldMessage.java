package com.crap.mukluk;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

// Represents messages from world -> activity or activity -> world
public class WorldMessage implements Parcelable
{
    public static final int MESSAGE_TYPE_ERROR = -1;
    public static final int MESSAGE_TYPE_STATUS_CHANGE = 0;
    public static final int MESSAGE_TYPE_TEXT = 1;

    public int type;
    public int currentStatus;
    public CharSequence text;

    public WorldMessage(int type, int currentStatus, CharSequence text)
    {
        this.type = type;
        this.currentStatus = currentStatus;
        this.text = text;
    }

    public WorldMessage(Parcel in)
    {
        type = in.readInt();
        currentStatus = in.readInt();
        text = in.readString();
    }

    public ContentValues getContentValues()
    {
        ContentValues vals = new ContentValues(3);

        vals.put("type", type);
        vals.put("currentStatus", currentStatus);
        vals.put("text", (String) text);

        return vals;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Parcelable.Creator<WorldMessage> CREATOR = new Parcelable.Creator<WorldMessage>()
    {
        public WorldMessage createFromParcel(Parcel in)
        {
            return new WorldMessage(in);
        }

        public WorldMessage[] newArray(int size)
        {
            return new WorldMessage[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(type);
        dest.writeInt(currentStatus);
        dest.writeString((String) text);
    }
}