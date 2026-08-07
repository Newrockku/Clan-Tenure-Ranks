package com.clantenure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Shared parsing for the free-text config fields.
 */
final class ConfigText
{
	private static final Pattern SEPARATOR = Pattern.compile("[\\r\\n,;]+");

	/**
	 * Matches the non-breaking space U+00A0 as well as ordinary whitespace. The client returns player
	 * names using non-breaking spaces, and Java's {@code \s} does not match those, so without this a
	 * name typed into config would never match the same name coming from the game.
	 */
	private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");

	private ConfigText()
	{
	}

	/**
	 * Splits a config field on new lines, commas and semicolons, dropping blanks and comment lines.
	 */
	static List<String> entries(String config)
	{
		final List<String> entries = new ArrayList<>();
		if (config == null)
		{
			return entries;
		}
		for (String raw : SEPARATOR.split(config))
		{
			final String trimmed = raw.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//"))
			{
				continue;
			}
			entries.add(trimmed);
		}
		return entries;
	}

	/**
	 * Case-insensitive, whitespace-collapsed form used to compare names and rank titles.
	 */
	static String normalize(String value)
	{
		return WHITESPACE.matcher(value.trim()).replaceAll(" ").trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Splits "key=value" or "key:value" at the first separator. Returns null if there isn't one.
	 */
	static String[] splitPair(String entry)
	{
		for (int i = 0; i < entry.length(); i++)
		{
			final char c = entry.charAt(i);
			if (c == '=' || c == ':')
			{
				return new String[]{entry.substring(0, i).trim(), entry.substring(i + 1).trim()};
			}
		}
		return null;
	}
}
