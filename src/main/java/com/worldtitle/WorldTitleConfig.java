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

	@ConfigItem(
		keyName = "showWorldActivity",
		name = "Show world activity",
		description = "Show the current world's listed activity in the RuneLite window title",
		position = 1
	)
	default boolean showWorldActivity()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showMembershipType",
		name = "Show membership type",
		description = "Show whether the current world is free or members",
		position = 2
	)
	default boolean showMembershipType()
	{
		return false;
	}
}
