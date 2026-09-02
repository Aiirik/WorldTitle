package com.worldtitle;

public enum WorldTitleMembershipStyle
{
	FULL_PARENS("(Members) / (Free)")
	{
		@Override
		String format(boolean members)
		{
			return members ? "(Members)" : "(Free)";
		}
	},
	SHORT_PARENS("(Members) / (F2P)")
	{
		@Override
		String format(boolean members)
		{
			return members ? "(Members)" : "(F2P)";
		}
	},
	NO_PARENS("Members / Free")
	{
		@Override
		String format(boolean members)
		{
			return members ? "Members" : "Free";
		}
	};

	private final String name;

	WorldTitleMembershipStyle(String name)
	{
		this.name = name;
	}

	abstract String format(boolean members);

	@Override
	public String toString()
	{
		return name;
	}
}
