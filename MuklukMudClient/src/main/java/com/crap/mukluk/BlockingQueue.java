package com.crap.mukluk;

import java.util.LinkedList;

public class BlockingQueue<T>
{
    private LinkedList<T> list;

    public BlockingQueue()
    {
        list = new LinkedList<T>();
    }

    public synchronized void addMessage(T msg)
    {
        list.addLast(msg);

        notifyAll();
    }

    // Gets a message from the queue. If none are available, blocks until some become available.
    // Returns null if thread interrupted
    // Returns null if queue will no longer be delivering messages
    public synchronized T getMessage()
    {
        while (list.size() == 0 /*&& !shutDownFlag*/)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                return null;
            }
        }

        return list.removeFirst();
    }
}