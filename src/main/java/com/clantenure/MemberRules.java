package com.clantenure;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Per-member exceptions: manual join dates that replace the in-game one, and members to leave out
 * of the report entirely.
 *
 * <p>Both are keyed on the normalised player name, so case and spacing do not have to match what
 * the game reports. As with {@link RankLadder}, parsing never throws - problems become warnings.
 */
public final class MemberRules
{
	/** Normalised names of members left out of the report entirely. */
	private final Set<String> ignored;

	/** Normalised name -> manual join date. */
	private final Map<String, LocalDate> joinDates;

	/** Normalised name -> the name as the user typed it, so warnings can quote it back. */
	private final Map<String, String> displayNames;

	private final List<String> warnings;

	private MemberRules(Set<String> ignored, Map<String, LocalDate> joinDates, Map<String, String> displayNames,
		List<String> warnings)
	{
		this.ignored = Collections.unmodifiableSet(ignored);
		this.joinDates = Collections.unmodifiableMap(joinDates);
		this.displayNames = Collections.unmodifiableMap(displayNames);
		this.warnings = Collections.unmodifiableList(warnings);
	}

	public static MemberRules parse(String ignoredConfig, String joinDateConfig, LocalDate today)
	{
		final List<String> warnings = new ArrayList<>();
		final Set<String> ignored = new LinkedHashSet<>();
		final Map<String, LocalDate> joinDates = new LinkedHashMap<>();
		final Map<String, String> displayNames = new LinkedHashMap<>();

		for (String entry : ConfigText.entries(ignoredConfig))
		{
			final String key = ConfigText.normalize(entry);
			displayNames.putIfAbsent(key, entry);
			if (!ignored.add(key))
			{
				warnings.add("\"" + entry + "\" is listed twice under ignored members.");
			}
		}

		for (String entry : ConfigText.entries(joinDateConfig))
		{
			parseJoinDate(entry, joinDates, ignored, displayNames, warnings, today);
		}

		return new MemberRules(ignored, joinDates, displayNames, warnings);
	}

	private static void parseJoinDate(String entry, Map<String, LocalDate> joinDates, Set<String> ignored,
		Map<String, String> displayNames, List<String> warnings, LocalDate today)
	{
		final String[] pair = ConfigText.splitPair(entry);
		if (pair == null)
		{
			warnings.add("Skipped join date \"" + entry + "\": expected the form Name=YYYY-MM-DD.");
			return;
		}

		// The documented form is Name=date, but a date is never a valid name so the two can be given
		// either way round without ambiguity.
		String name = pair[0];
		LocalDate date = tryParseDate(pair[1]);
		if (date == null)
		{
			date = tryParseDate(pair[0]);
			name = pair[1];
		}

		if (date == null)
		{
			warnings.add("Skipped join date \"" + entry + "\": use the form Name=YYYY-MM-DD, for example "
				+ "Zezima=2015-03-14.");
			return;
		}
		if (name.isEmpty())
		{
			warnings.add("Skipped join date \"" + entry + "\": no player name given.");
			return;
		}
		if (date.isAfter(today))
		{
			warnings.add("Manual join date for \"" + name + "\" is in the future, so their tenure will be 0.");
		}

		final String key = ConfigText.normalize(name);
		displayNames.putIfAbsent(key, name);

		if (ignored.contains(key))
		{
			warnings.add("\"" + name + "\" has a manual join date but is also ignored, so the date is unused.");
		}

		if (joinDates.put(key, date) != null)
		{
			warnings.add("\"" + name + "\" has more than one manual join date; using " + date + ".");
		}
	}

	@Nullable
	private static LocalDate tryParseDate(String value)
	{
		final String cleaned = value.trim().replace('/', '-');
		if (cleaned.isEmpty())
		{
			return null;
		}
		try
		{
			return LocalDate.parse(cleaned);
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}

	public boolean isIgnored(String name)
	{
		return name != null && ignored.contains(ConfigText.normalize(name));
	}

	/**
	 * The manual join date for this member, or null to use the one the game reports.
	 */
	@Nullable
	public LocalDate joinDateFor(String name)
	{
		return name == null ? null : joinDates.get(ConfigText.normalize(name));
	}

	public boolean isEmpty()
	{
		return ignored.isEmpty() && joinDates.isEmpty();
	}

	/**
	 * Normalised name to the name as typed, for every member referenced by either list. The service
	 * uses this to warn about names matching nobody in the clan - a typo would otherwise fail
	 * silently.
	 */
	public Map<String, String> referencedNames()
	{
		return displayNames;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}
}
