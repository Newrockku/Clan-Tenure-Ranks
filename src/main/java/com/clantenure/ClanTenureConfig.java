package com.clantenure;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ClanTenureConfig.GROUP)
public interface ClanTenureConfig extends Config
{
	String GROUP = "clantenureranks";

	String DEFAULT_ELIGIBLE_RANKS = "Jade, Red Topaz, Sapphire, Emerald, Ruby, Diamond, Dragonstone, Onyx, Zenyte";

	String DEFAULT_THRESHOLDS =
		"120=Red Topaz\n"
			+ "240=Sapphire\n"
			+ "365=Emerald\n"
			+ "485=Ruby\n"
			+ "605=Diamond\n"
			+ "730=Dragonstone\n"
			+ "910=Onyx\n"
			+ "1095=Zenyte";

	@ConfigSection(
		name = "Rank ladder",
		description = "Which ranks tenure applies to, and how long each one takes to earn.",
		position = 0
	)
	String ladderSection = "ladderSection";

	@ConfigSection(
		name = "Notifications",
		description = "Alerts when a member newly qualifies for a promotion.",
		position = 1
	)
	String notificationsSection = "notificationsSection";

	@ConfigSection(
		name = "Member exceptions",
		description = "Per-member overrides: manual join dates, and members to leave out entirely.",
		position = 2
	)
	String exceptionsSection = "exceptionsSection";

	@ConfigItem(
		position = 1,
		keyName = "eligibleRanks",
		name = "Eligible ranks",
		description = "The tenure ladder, lowest rank first, separated by commas or new lines."
			+ "<br><br>Only members holding one of these ranks are checked - staff and event ranks are"
			+ " left alone. The first rank listed is the entry rank and needs no threshold.",
		section = ladderSection
	)
	default String eligibleRanks()
	{
		return DEFAULT_ELIGIBLE_RANKS;
	}

	@ConfigItem(
		position = 2,
		keyName = "tenureThresholds",
		name = "Tenure thresholds",
		description = "Days required for each rank, one per line, in the form 120=Red Topaz."
			+ "<br><br>Every name here must also appear in Eligible ranks.",
		section = ladderSection
	)
	default String tenureThresholds()
	{
		return DEFAULT_THRESHOLDS;
	}

	@ConfigItem(
		position = 3,
		keyName = "notifyOnPromotion",
		name = "Notify when someone becomes due",
		description = "Show a RuneLite notification when a member newly qualifies for a promotion."
			+ "<br><br>Only fires for members who were not already due on the last refresh, so it will"
			+ " not spam you every time the panel updates. Uses your RuneLite-wide notification"
			+ " settings (tray popup, sound, etc.) for how it is shown.",
		section = notificationsSection
	)
	default boolean notifyOnPromotion()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "joinDateOverrides",
		name = "Manual join dates",
		description = "Replace a member's in-game join date, one per line, in the form"
			+ " <br>Zezima=2015-03-14"
			+ "<br><br>Use this when the in-game date is wrong - someone who left and rejoined, or was"
			+ " in the clan before it was remade. Dates must be YYYY-MM-DD.",
		section = exceptionsSection
	)
	default String joinDateOverrides()
	{
		return "";
	}

	@ConfigItem(
		position = 5,
		keyName = "ignoredMembers",
		name = "Ignored members",
		description = "Members to leave out of the report entirely, separated by commas or new lines."
			+ "<br><br>They are not counted and never appear as due a promotion. Useful for alts and"
			+ " accounts you handle by hand.",
		section = exceptionsSection
	)
	default String ignoredMembers()
	{
		return "";
	}
}
