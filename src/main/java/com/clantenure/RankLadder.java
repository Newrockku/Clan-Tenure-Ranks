package com.clantenure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The clan's tenure ladder, built from the two config fields.
 *
 * <p>The <em>eligible ranks</em> field supplies both the set of ranks that tenure applies to and
 * their order, lowest first. The <em>thresholds</em> field supplies the days required for each.
 * A rank with no threshold entry sits at 0 days, which is how the entry rank (e.g. Jade) is
 * expressed.
 *
 * <p>Parsing never throws. Anything it cannot make sense of is collected into {@link #getWarnings()}
 * and surfaced in the panel, because a silently mis-parsed ladder would produce confident but
 * wrong promotion advice.
 */
public final class RankLadder
{
	private final List<String> names;
	private final int[] thresholds;
	private final Map<String, Integer> indexByName;
	private final List<String> warnings;

	private RankLadder(List<String> names, int[] thresholds, Map<String, Integer> indexByName, List<String> warnings)
	{
		this.names = Collections.unmodifiableList(names);
		this.thresholds = thresholds;
		this.indexByName = Collections.unmodifiableMap(indexByName);
		this.warnings = Collections.unmodifiableList(warnings);
	}

	public static RankLadder parse(String eligibleRanksConfig, String thresholdsConfig)
	{
		final List<String> warnings = new ArrayList<>();
		final List<String> names = new ArrayList<>();
		final Map<String, Integer> indexByName = new LinkedHashMap<>();

		for (String entry : ConfigText.entries(eligibleRanksConfig))
		{
			final String key = ConfigText.normalize(entry);
			if (indexByName.containsKey(key))
			{
				warnings.add("Duplicate eligible rank \"" + entry + "\" was ignored.");
				continue;
			}
			indexByName.put(key, names.size());
			names.add(entry);
		}

		final int[] thresholds = new int[names.size()];
		final boolean[] explicit = new boolean[names.size()];

		for (String entry : ConfigText.entries(thresholdsConfig))
		{
			parseThreshold(entry, names, indexByName, thresholds, explicit, warnings);
		}

		if (names.isEmpty())
		{
			warnings.add("No eligible ranks are configured, so there is nothing to compare against.");
		}
		else
		{
			if (explicit[0] && thresholds[0] > 0)
			{
				warnings.add("\"" + names.get(0) + "\" is the lowest eligible rank, so it is treated as the entry "
					+ "rank at 0 days and its " + thresholds[0] + "-day threshold is ignored.");
			}

			for (int i = 1; i < names.size(); i++)
			{
				if (!explicit[i])
				{
					warnings.add("\"" + names.get(i) + "\" has no tenure threshold, so it is treated as 0 days.");
				}
			}

			int previous = -1;
			for (int i = 0; i < names.size(); i++)
			{
				if (!explicit[i])
				{
					continue;
				}
				if (previous >= 0 && thresholds[i] <= thresholds[previous])
				{
					warnings.add("\"" + names.get(i) + "\" (" + thresholds[i] + "d) requires no more time than the "
						+ "lower rank \"" + names.get(previous) + "\" (" + thresholds[previous] + "d) - check the "
						+ "order of your eligible ranks.");
				}
				previous = i;
			}
		}

		return new RankLadder(names, thresholds, indexByName, warnings);
	}

	private static void parseThreshold(String entry, List<String> names, Map<String, Integer> indexByName,
		int[] thresholds, boolean[] explicit, List<String> warnings)
	{
		final String[] pair = ConfigText.splitPair(entry);
		if (pair == null)
		{
			warnings.add("Skipped \"" + entry + "\": expected the form 120=Red Topaz.");
			return;
		}

		// The documented form is days=Name, but Name=days is unambiguous so we accept it too.
		Integer days = tryParseInt(pair[0]);
		String rankName = pair[1];
		if (days == null)
		{
			days = tryParseInt(pair[1]);
			rankName = pair[0];
		}

		if (days == null)
		{
			warnings.add("Skipped \"" + entry + "\": neither side is a number of days.");
			return;
		}
		if (rankName.isEmpty())
		{
			warnings.add("Skipped \"" + entry + "\": no rank name given.");
			return;
		}

		final Integer index = indexByName.get(ConfigText.normalize(rankName));
		if (index == null)
		{
			warnings.add("Threshold \"" + entry + "\" names a rank that is not in your eligible ranks - ignored.");
			return;
		}
		if (explicit[index])
		{
			warnings.add("Duplicate threshold for \"" + names.get(index) + "\"; kept " + thresholds[index] + " days.");
			return;
		}
		if (days < 0)
		{
			warnings.add("Threshold for \"" + names.get(index) + "\" is negative; treated as 0 days.");
			days = 0;
		}

		thresholds[index] = days;
		explicit[index] = true;
	}

	private static Integer tryParseInt(String value)
	{
		try
		{
			return Integer.valueOf(value.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/**
	 * The highest ladder rank earned by the given tenure. Floors at the entry rank, so a member who
	 * is on the ladder always earns at least its lowest rung.
	 */
	public int earnedIndex(long tenureDays)
	{
		for (int i = names.size() - 1; i >= 1; i--)
		{
			if (tenureDays >= thresholds[i])
			{
				return i;
			}
		}
		return 0;
	}

	public OptionalInt indexOf(String rankName)
	{
		if (rankName == null)
		{
			return OptionalInt.empty();
		}
		final Integer index = indexByName.get(ConfigText.normalize(rankName));
		return index == null ? OptionalInt.empty() : OptionalInt.of(index);
	}

	public boolean isUsable()
	{
		return !names.isEmpty();
	}

	public int size()
	{
		return names.size();
	}

	public String nameAt(int index)
	{
		return names.get(index);
	}

	public int thresholdAt(int index)
	{
		return index == 0 ? 0 : thresholds[index];
	}

	public List<String> getNames()
	{
		return names;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}
}
