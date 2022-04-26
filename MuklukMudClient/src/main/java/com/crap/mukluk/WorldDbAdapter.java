package com.crap.mukluk;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * Version history:
 * 2 - added 'logging enabled'
 * 3 - added auto connect and post-login commands
 * 4 - added encoding
 * 5 - added ansiColorEnabled, removed references to autoConnect in settings
 */
public class WorldDbAdapter
{
    //private static final String TAG = "SQLiteOpenHelper";
    private static final int DATABASE_VERSION = 5;

    private SQLiteDatabase db;
    private WorldDatabaseHelper dbHelper;
    private Context context;

    public WorldDbAdapter(Context context)
    {
        this.context = context;
    }

    public WorldDbAdapter open() throws SQLException
    {
        dbHelper = new WorldDatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close()
    {
        db.close();
        db = null;

        dbHelper.close();
        dbHelper = null;
    }

    // world added won't have a dbID because it's new
    public void addWorldToDatabase(World world)
    {
        long newID = db.insert("tblWorld", null, world.getContentValues());
        addPostLoginCommandsToDatabase((int) newID, world.postLoginCommands);
    }

    public void deleteWorldFromDatabase(long id)
    {
        db.execSQL("DELETE FROM tblWorld WHERE _id = " + id + ";");
        db.execSQL("DELETE FROM tblWorldPostLoginCommand WHERE _id = " + id + ";");
    }

    public World[] getAllWorlds()
    {
        World[] dbWorlds = new World[0];
        Cursor cursor = db.query("tblWorld", new String[] {"_id", "name", "host", "port", "loggingEnabled", "ansiColorEnabled", "encoding"}, null, null, null, null, null);

        if (cursor != null)
        {
            int rowCount = cursor.getCount();
            dbWorlds = new World[rowCount];
            cursor.moveToFirst();

            for (int i = 0; i < rowCount; i++)
            {
                dbWorlds[i] = new World(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getInt(4) > 0 ? true : false,
                    cursor.getInt(5) > 0 ? true : false,
                    cursor.getString(6),
                    getPostLoginCommandsForWorldFromDatabase(cursor.getInt(0)));
                cursor.moveToNext();
            }

            cursor.close();
            cursor = null;
        }

        return dbWorlds;
    }

    public World getWorld(int id)
    {
        Cursor cursor = db.query(true, "tblWorld", new String[] {"_id", "name", "host", "port", "loggingEnabled", "ansiColorEnabled", "encoding"}, "_id = " + id, null, null, null, null, null);
        World world;

        if (cursor != null)
            cursor.moveToFirst();
        else
            return null;

        world = new World(
            cursor.getInt(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getInt(3),
            cursor.getInt(4) > 0 ? true : false,
            cursor.getInt(5) > 0 ? true : false,
            cursor.getString(6),
            getPostLoginCommandsForWorldFromDatabase(id));

        cursor.close();
        cursor = null;

        return world;
    }

    public void updateWorldInDatabase(World world)
    {
        ContentValues cv = world.getContentValues();
        db.update("tblWorld", cv, "_id = " + world.dbID, null);
        addPostLoginCommandsToDatabase(world.dbID, world.postLoginCommands);
    }

    private void addPostLoginCommandsToDatabase(int worldID, List<String> postLoginCommands)
    {
        db.execSQL("DELETE FROM tblWorldPostLoginCommand WHERE _id = " + worldID);

        for (String cmd : postLoginCommands)
        {
            ContentValues commands = new ContentValues(2);
            commands.put("_id", worldID);
            commands.put("command", cmd);
            db.insert("tblWorldPostLoginCommand", null, commands);
        }
    }

    private List<String> getPostLoginCommandsForWorldFromDatabase(int worldID)
    {
        List<String> postLoginCommands = new ArrayList<String>();

        Cursor cursor = db.query(true, "tblWorldPostLoginCommand", new String[] {"command"},  "_id = " + worldID, null, null, null, null, null);
        if (cursor != null)
        {
            int rowCount = cursor.getCount();
            cursor.moveToFirst();

            for (int i = 0; i < rowCount; i++)
            {
                postLoginCommands.add(cursor.getString(0));
                cursor.moveToNext();
            }
        }

        cursor.close();
        cursor = null;

        return postLoginCommands;
    }

    private static class WorldDatabaseHelper extends SQLiteOpenHelper
    {
        WorldDatabaseHelper(Context context)
        {
            super(context, "tblWorld", null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE tblWorld (_id integer primary key autoincrement, name text, host text, port integer, loggingEnabled integer default 0, autoConnect integer default 0, ansiColorEnabled integer default 1, encoding text default 'UTF-8');");
            db.execSQL("CREATE TABLE tblWorldPostLoginCommand (_id integer primary key, command text);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            if (oldVersion < 2)
                db.execSQL("ALTER TABLE tblWorld ADD loggingEnabled integer DEFAULT 0;");

            if (oldVersion < 3)
            {
                db.execSQL("ALTER TABLE tblWorld ADD autoConnect integer DEFAULT 0;");
                db.execSQL("CREATE TABLE tblWorldPostLoginCommand (_id integer primary key, command text);");
            }

            if (oldVersion < 4)
            {
                db.execSQL("ALTER TABLE tblWorld ADD encoding text DEFAULT 'UTF-8';");
            }

            if (oldVersion < 5)
            {
                db.execSQL("ALTER TABLE tblWorld ADD ansiColorEnabled integer DEFAULT 1;");
            }
        }
    }
}
