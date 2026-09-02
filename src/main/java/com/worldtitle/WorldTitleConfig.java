package com.worldtitle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("world-title")
public interface WorldTitleConfig extends Config
{
	@ConfigItem(
		keyName = "worldFormat",
		name = "World format",
		description = "How the current world is displayed in the RuneLite window title",
		position = 0
	)
	default WorldTitleFormat worldFormat()
	{
		return WorldTitleFormat.WORLD_PREFIX;
	}
}
