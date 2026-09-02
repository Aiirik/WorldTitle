package com.worldtitle;

public enum WorldTitleFormat
{
	WORLD_PREFIX("W301")
	{
		@Override
		String format(int world)
		{
			return "W" + world;
		}
	},
	WORLD_WORD("World 301")
	{
		@Override
		String format(int world)
		{
			return "World " + world;
		}
	},
	BRACKETS("[301]")
	{
		@Override
		String format(int world)
		{
			return "[" + world + "]";
		}
	},
	NUMBER("301")
	{
		@Override
		String format(int world)
		{
			return Integer.toString(world);
		}
	};

	private final String name;

	WorldTitleFormat(String name)
	{
		this.name = name;
	}

	abstract String format(int world);

	@Override
	public String toString()
	{
		return name;
	}
}
