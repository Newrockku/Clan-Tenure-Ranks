package com.clantenure;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;

/**
 * Reads the clan roster out of the client and evaluates every member against the tenure ladder.
 */
@Slf4j
@Singleton
public class ClanTenureService
{
	/** ClanRank values run -1 (guest) to 127 (JMod); we sweep the range to enumerate clan titles. */
	private static final int MIN_RANK = -1;
	private static final int MAX_RANK = 127;

	@Inject
	private Client client;

	/**
	 * Builds a snapshot of the whole clan. Must be called on the client thread.
	 */
	public ClanSnapshot snapshot(RankLadder ladder, MemberRules rules)
	{
		final List<String> warnings = new ArrayList<>(ladder.getWarnings());
		warnings.addAll(rules.getWarnings());

		if (!ladder.isUsable())
		{
			return ClanSnapshot.empty(ClanSnapshot.State.NO_LADDER, warnings);
		}

		final GameState gameState = client.getGameState();
		final boolean loggedIn = gameState == GameState.LOGGED_IN || gameState == GameState.LOADING;

		final ClanSettings settings = client.getClanSettings();
		if (settings == null)
		{
			return ClanSnapshot.empty(loggedIn ? ClanSnapshot.State.NO_CLAN : ClanSnapshot.State.LOGGED_OUT, warnings);
		}

		warnings.addAll(validateAgainstClanTitles(ladder, settings));

		final LocalDate today = LocalDate.now(ZoneOffset.UTC);
		final List<ClanMember> clanMembers = settings.getMembers();
		final List<MemberTenure> rows = new ArrayList<>();
		final Set<String> rosterNames = new HashSet<>();

		if (clanMembers != null)
		{
			for (ClanMember member : clanMembers)
			{
				if (member == null || member.getName() == null)
				{
					continue;
				}

				final String name = member.getName();
				rosterNames.add(ConfigText.normalize(name));

				if (rules.isIgnored(name))
				{
					rows.add(new MemberTenure(name, member.getJoinDate(), 0, titleOf(settings, member), null,
						TenureStatus.IGNORED, false));
					continue;
				}

				final LocalDate override = rules.joinDateFor(name);
				final LocalDate joinDate = override != null ? override : member.getJoinDate();
				rows.add(evaluate(name, joinDate, titleOf(settings, member), ladder, today, override != null));
			}
		}

		warnings.addAll(validateAgainstRoster(rules, rosterNames));

		// If the roster loaded but carries no dates at all, tenure is unknowable - say so rather than
		// reporting everyone as a 0-day member.
		final boolean anyJoinDate = rows.stream()
			.anyMatch(r -> r.getStatus() != TenureStatus.IGNORED && r.getJoinDate() != null);
		if (!rows.isEmpty() && !anyJoinDate)
		{
			log.debug("Clan tenure: roster of {} loaded but no member has a join date", rows.size());
			return new ClanSnapshot(ClanSnapshot.State.NO_JOIN_DATES, settings.getName(), rows, warnings);
		}

		final ClanSnapshot snapshot = new ClanSnapshot(ClanSnapshot.State.OK, settings.getName(), rows, warnings);
		logSnapshot(snapshot);
		return snapshot;
	}

	/**
	 * One line per refresh, so roster problems can be diagnosed from a client log rather than from a
	 * description of what the panel looked like.
	 */
	private static void logSnapshot(ClanSnapshot snapshot)
	{
		if (!log.isDebugEnabled())
		{
			return;
		}
		final long withJoinDates = snapshot.getMembers().stream().filter(m -> m.getJoinDate() != null).count();
		log.debug("Clan tenure: {} members, {} with join dates, {} tracked, {} due, {} over-ranked, {} ignored, "
				+ "{} config warnings",
			snapshot.getMembers().size(), withJoinDates, snapshot.trackedCount(),
			snapshot.count(TenureStatus.DUE_PROMOTION), snapshot.count(TenureStatus.OVER_RANKED),
			snapshot.count(TenureStatus.IGNORED), snapshot.getWarnings().size());
	}

	@Nullable
	private static String titleOf(ClanSettings settings, ClanMember member)
	{
		final ClanRank rank = member.getRank();
		if (rank == null)
		{
			return null;
		}
		final ClanTitle title = settings.titleForRank(rank);
		return title == null ? null : title.getName();
	}

	/**
	 * Compares each configured rank name against the clan's real titles, so a typo shows up as a
	 * warning instead of quietly marking everyone "not tracked".
	 */
	private static List<String> validateAgainstClanTitles(RankLadder ladder, ClanSettings settings)
	{
		final Set<String> actual = new HashSet<>();
		for (int rank = MIN_RANK; rank <= MAX_RANK; rank++)
		{
			final ClanTitle title = settings.titleForRank(new ClanRank(rank));
			if (title != null && title.getName() != null)
			{
				actual.add(ConfigText.normalize(title.getName()));
			}
		}

		// Titles not loaded yet - don't cry wolf.
		if (actual.isEmpty())
		{
			return Collections.emptyList();
		}

		final List<String> warnings = new ArrayList<>();
		for (String name : ladder.getNames())
		{
			if (!actual.contains(ConfigText.normalize(name)))
			{
				warnings.add("\"" + name + "\" is not a rank title in this clan - check the spelling.");
			}
		}
		return warnings;
	}

	/**
	 * A misspelled name in the ignored or manual join date lists would silently do nothing, so call
	 * out any that match nobody in the clan.
	 */
	static List<String> validateAgainstRoster(MemberRules rules, Set<String> rosterNames)
	{
		if (rules.isEmpty() || rosterNames.isEmpty())
		{
			return Collections.emptyList();
		}

		final List<String> warnings = new ArrayList<>();
		for (Map.Entry<String, String> entry : rules.referencedNames().entrySet())
		{
			if (!rosterNames.contains(entry.getKey()))
			{
				warnings.add("\"" + entry.getValue() + "\" is listed under ignored members or manual join dates, "
					+ "but nobody in the clan has that name.");
			}
		}
		return warnings;
	}

	/**
	 * Evaluates a single member. Pure: no client access, so this is what the unit tests drive.
	 */
	static MemberTenure evaluate(String name, @Nullable LocalDate joinDate, @Nullable String currentRank,
		RankLadder ladder, LocalDate today)
	{
		return evaluate(name, joinDate, currentRank, ladder, today, false);
	}

	static MemberTenure evaluate(String name, @Nullable LocalDate joinDate, @Nullable String currentRank,
		RankLadder ladder, LocalDate today, boolean joinDateOverridden)
	{
		final OptionalInt currentIndex = ladder.indexOf(currentRank);

		// Off-ladder ranks (staff, event ranks) are ignored outright, join date or not.
		if (!currentIndex.isPresent())
		{
			return new MemberTenure(name, joinDate, tenureDays(joinDate, today), currentRank, null,
				TenureStatus.NOT_TRACKED, joinDateOverridden);
		}

		if (joinDate == null)
		{
			return new MemberTenure(name, null, 0, currentRank, null, TenureStatus.UNKNOWN, false);
		}

		final long days = tenureDays(joinDate, today);
		final int earnedIndex = ladder.earnedIndex(days);

		final TenureStatus status;
		if (currentIndex.getAsInt() < earnedIndex)
		{
			status = TenureStatus.DUE_PROMOTION;
		}
		else if (currentIndex.getAsInt() > earnedIndex)
		{
			status = TenureStatus.OVER_RANKED;
		}
		else
		{
			status = TenureStatus.CORRECT;
		}

		return new MemberTenure(name, joinDate, days, currentRank, ladder.nameAt(earnedIndex), status,
			joinDateOverridden);
	}

	private static long tenureDays(@Nullable LocalDate joinDate, LocalDate today)
	{
		if (joinDate == null)
		{
			return 0;
		}
		return Math.max(0, ChronoUnit.DAYS.between(joinDate, today));
	}
}
