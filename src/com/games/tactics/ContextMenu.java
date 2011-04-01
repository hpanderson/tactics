package com.games.tactics;

import java.util.ArrayList;
import java.util.Iterator;

public class ContextMenu
{
	public ContextMenu()
	{
		mCommandIds = new ArrayList<ContextCommand>();
	}
	
	public void AddCommand(ContextCommand inCommandId)
	{
		mCommandIds.add(inCommandId);
	}
	
	public int size() { return mCommandIds.size(); }
	public Iterator<ContextCommand> iterator() { return mCommandIds.iterator(); }
	
	private ArrayList<ContextCommand> mCommandIds;

	public enum ContextCommand
	{
		ATTACK { public String toString() { return "Attack"; } },
		MOVE { public String toString() { return "Move"; } },
		INVENTORY { public String toString() { return "Inventory"; } },
		STATUS { public String toString() { return "Status"; } }
	}
}
