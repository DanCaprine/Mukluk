package com.crap.mukluk;

import java.util.LinkedList;

// A queue where items are removed if the list exceeds a certain size
@SuppressWarnings("serial")
public class FixedSizeList<E> extends LinkedList<E>
{
	int _maximumSize;
	
	public FixedSizeList(int maximumSize)
	{
		super();
		
		_maximumSize = maximumSize;
	}
	
	public boolean add(E item)
	{
		boolean addResult = super.add(item);
		
		if (this.size() > _maximumSize)
			this.removeFirst();
		
		return addResult;
	}
	
	public void addFirst(E item)
	{
		super.addFirst(item);
		
		if (this.size() > _maximumSize)
			this.removeLast();
	}
	
	public void setMaximumSize(int newMaximumSize)
	{
		int currentSize = this.size();
		
		if (currentSize > newMaximumSize)
			this.subList(0, currentSize - newMaximumSize).clear();
		
		_maximumSize = newMaximumSize;
	}
}
