package com.clantenure;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tracks which members were due a promotion on the last successful refresh, so the plugin can
 * notify only about members who newly cross a threshold - not the whole roster every time the
 * panel refreshes.
 */
class PromotionWatcher
{
	private static final int MAX_NAMES_IN_MESSAGE = 5;

	private boolean seeded;
	private String clanName;
	private Set<String> lastDue = Collections.emptySet();

	/**
	 * Returns the members who are due a promotion now but were not on the last successful refresh of
	 * the same clan. The very first successful refresh, and any refresh for a different clan than
	 * last time (an account switch), seed the baseline silently rather than reporting everyone who
	 * was already overdue. A refresh that failed to load the roster (logged out, no clan, etc.)
	 * leaves the baseline untouched, so a brief blip does not reset it.
	 */
	List<MemberTenure> update(ClanSnapshot snapshot)
	{
		if (snapshot.getState() != ClanSnapshot.State.OK)
		{
			return Collections.emptyList();
		}

		final boolean sameClan = seeded && Objects.equals(clanName, snapshot.getClanName());

		final List<MemberTenure> newlyDue = !sameClan ? Collections.emptyList()
			: snapshot.getMembers().stream()
				.filter(m -> m.getStatus() == TenureStatus.DUE_PROMOTION)
				.filter(m -> !lastDue.contains(ConfigText.normalize(m.getName())))
				.collect(Collectors.toList());

		clanName = snapshot.getClanName();
		seeded = true;
		lastDue = snapshot.getMembers().stream()
			.filter(m -> m.getStatus() == TenureStatus.DUE_PROMOTION)
			.map(m -> ConfigText.normalize(m.getName()))
			.collect(Collectors.toCollection(HashSet::new));

		return newlyDue;
	}

	/**
	 * A notification message for the given newly-due members. Long lists collapse into a count
	 * rather than a wall of names, since a bulk change (e.g. editing thresholds) can otherwise turn
	 * one config edit into a dozen tray popups.
	 */
	static String message(List<MemberTenure> newlyDue)
	{
		if (newlyDue.size() == 1)
		{
			final MemberTenure member = newlyDue.get(0);
			return member.getName() + " is now due a promotion to " + member.getEarnedRank() + ".";
		}
		if (newlyDue.size() <= MAX_NAMES_IN_MESSAGE)
		{
			final String names = newlyDue.stream().map(MemberTenure::getName).collect(Collectors.joining(", "));
			return newlyDue.size() + " members are now due a promotion: " + names;
		}
		return newlyDue.size() + " members are now due a promotion - check the Clan Tenure Ranks panel.";
	}
}
