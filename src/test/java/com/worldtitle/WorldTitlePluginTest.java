package com.worldtitle;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WorldTitlePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WorldTitlePlugin.class);
		RuneLite.main(args);
	}
}
