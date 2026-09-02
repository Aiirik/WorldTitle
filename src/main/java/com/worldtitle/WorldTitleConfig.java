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
		keyName = "showWorldRegion",
		name = "Show world region",
		description = "Show the current world's region in the RuneLite window title",
		position = 2
	)
	default boolean showWorldRegion()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showPlayerCount",
		name = "Show player count",
		description = "Show the current world's player count in the RuneLite window title",
		position = 3
	)
	default boolean showPlayerCount()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showMembershipType",
		name = "Show membership type",
		description = "Show whether the current world is free or members",
		position = 4
	)
	default boolean showMembershipType()
	{
		return false;
	}

	@ConfigItem(
		keyName = "membershipStyle",
		name = "Membership style",
		description = "How free and members worlds are displayed",
		position = 5
	)
	default WorldTitleMembershipStyle membershipStyle()
	{
		return WorldTitleMembershipStyle.FULL_PARENS;
	}

	@ConfigItem(
		keyName = "separator",
		name = "Separator",
		description = "Separator used between world title details",
		position = 6
	)
	default WorldTitleSeparator separator()
	{
		return WorldTitleSeparator.DASH;
	}
}
