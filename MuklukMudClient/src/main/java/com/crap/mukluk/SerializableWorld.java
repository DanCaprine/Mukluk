package com.crap.mukluk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Weird errors occur when a class is both parcelable and serializable, so use this to export and import World class
public class SerializableWorld implements Serializable
{
    private static final long serialVersionUID = 358488681588640820L;
    public String name;
    public String host;
    public int port = 0;
    public boolean loggingEnabled = false;
    public boolean ansiColorEnabled = true;
    public String encodingName;
    public List<String> postLoginCommands = new ArrayList<String>();

    public SerializableWorld(World world)
    {
        this.name = world.name;
        this.host = world.host;
        this.port = world.port;
        this.loggingEnabled = world.loggingEnabled;
        this.ansiColorEnabled = world.ansiColorEnabled;
        this.encodingName = world.encoding.name();
        postLoginCommands.addAll(world.postLoginCommands);
    }

    public World getAsWorld()
    {
        World world = new World();
        world.name = name;
        world.host = host;
        world.port = port;
        world.loggingEnabled = loggingEnabled;
        world.ansiColorEnabled = ansiColorEnabled;
        world.setCharset(encodingName);
        world.postLoginCommands.addAll(postLoginCommands);

        return world;
    }
}
