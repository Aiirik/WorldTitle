package com.worldtitle;

public enum WorldTitleSeparator
{
	DASH(" - ", "Dash"),
	PIPE(" | ", "Pipe"),
	COLON(": ", "Colon");

	private final String separator;
	private final String name;

	WorldTitleSeparator(String separator, String name)
	{
		this.separator = separator;
		this.name = name;
	}

	String separator()
	{
		return separator;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
