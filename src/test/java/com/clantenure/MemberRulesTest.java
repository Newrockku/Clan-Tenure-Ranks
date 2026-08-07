package com.clantenure;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MemberRulesTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 6);

	private static MemberRules parse(String ignored, String joinDates)
	{
		return MemberRules.parse(ignored, joinDates, TODAY);
	}

	@Test
	public void emptyConfigIgnoresNobodyAndOverridesNothing()
	{
		final MemberRules rules = parse("", "");

		assertTrue(rules.isEmpty());
		assertFalse(rules.isIgnored("Zezima"));
		assertNull(rules.joinDateFor("Zezima"));
		assertEquals(0, rules.getWarnings().size());
	}

	@Test
	public void nullConfigIsTreatedAsEmpty()
	{
		final MemberRules rules = MemberRules.parse(null, null, TODAY);

		assertTrue(rules.isEmpty());
	}

	@Test
	public void ignoredMembersAcceptCommasAndNewLines()
	{
		final MemberRules rules = parse("Alt One, Alt Two\nAlt Three", "");

		assertTrue(rules.isIgnored("Alt One"));
		assertTrue(rules.isIgnored("Alt Two"));
		assertTrue(rules.isIgnored("Alt Three"));
		assertFalse(rules.isIgnored("Zezima"));
	}

	@Test
	public void nameMatchingIgnoresCaseAndSpacing()
	{
		final MemberRules rules = parse("Iron Mammal", "zezima=2015-03-14");

		assertTrue(rules.isIgnored("iron   mammal"));
		assertTrue(rules.isIgnored("IRON MAMMAL"));
		assertEquals(LocalDate.of(2015, 3, 14), rules.joinDateFor("Zezima"));
	}

	/**
	 * The client hands back player names using non-breaking spaces, so config typed with ordinary
	 * spaces has to match them.
	 */
	@Test
	public void nameMatchingHandlesNonBreakingSpacesFromTheClient()
	{
		final MemberRules rules = parse("Iron Mammal", "Iron Mammal=2015-03-14");

		// U+00A0, which is what ClanMember.getName() actually returns in place of a space.
		final String fromClient = "Iron\u00A0Mammal";

		assertTrue(rules.isIgnored(fromClient));
		assertEquals(LocalDate.of(2015, 3, 14), rules.joinDateFor(fromClient));
	}

	@Test
	public void manualJoinDateParsesIsoAndSlashForms()
	{
		final MemberRules rules = parse("", "Zezima=2015-03-14\nWoox=2018/11/02");

		assertEquals(LocalDate.of(2015, 3, 14), rules.joinDateFor("Zezima"));
		assertEquals(LocalDate.of(2018, 11, 2), rules.joinDateFor("Woox"));
		assertEquals(0, rules.getWarnings().size());
	}

	@Test
	public void acceptsDateEqualsNameAsWellAsNameEqualsDate()
	{
		final MemberRules rules = parse("", "2015-03-14=Zezima");

		assertEquals(LocalDate.of(2015, 3, 14), rules.joinDateFor("Zezima"));
		assertEquals(0, rules.getWarnings().size());
	}

	@Test
	public void warnsOnUnparseableDateRatherThanGuessingTheFormat()
	{
		final MemberRules rules = parse("", "Zezima=14/03/2015\nWoox=last tuesday\nBoaty");

		assertNull(rules.joinDateFor("Zezima"));
		assertNull(rules.joinDateFor("Woox"));
		assertTrue(containing(rules.getWarnings(), "Name=YYYY-MM-DD"));
		assertEquals(3, rules.getWarnings().size());
	}

	@Test
	public void warnsOnFutureJoinDate()
	{
		final MemberRules rules = parse("", "Zezima=2027-01-01");

		assertTrue(containing(rules.getWarnings(), "in the future"));
	}

	@Test
	public void warnsOnDuplicateIgnoredEntry()
	{
		final MemberRules rules = parse("Zezima, zezima", "");

		assertTrue(containing(rules.getWarnings(), "listed twice"));
	}

	@Test
	public void warnsOnConflictingJoinDatesAndKeepsTheLast()
	{
		final MemberRules rules = parse("", "Zezima=2015-03-14\nZezima=2016-01-01");

		assertEquals(LocalDate.of(2016, 1, 1), rules.joinDateFor("Zezima"));
		assertTrue(containing(rules.getWarnings(), "more than one manual join date"));
	}

	@Test
	public void warnsWhenAMemberIsBothIgnoredAndGivenAJoinDate()
	{
		final MemberRules rules = parse("Zezima", "Zezima=2015-03-14");

		assertTrue(containing(rules.getWarnings(), "also ignored"));
	}

	@Test
	public void commentLinesAreSkipped()
	{
		final MemberRules rules = parse("# alts\nAlt One", "// rejoined members\nZezima=2015-03-14");

		assertTrue(rules.isIgnored("Alt One"));
		assertFalse(rules.isIgnored("# alts"));
		assertEquals(LocalDate.of(2015, 3, 14), rules.joinDateFor("Zezima"));
	}

	@Test
	public void namesMatchingNobodyInTheClanAreReported()
	{
		final MemberRules rules = parse("Typoed Nmae", "Zezima=2015-03-14");
		final Set<String> roster = names("zezima", "woox");

		final List<String> warnings = ClanTenureService.validateAgainstRoster(rules, roster);

		assertEquals(1, warnings.size());
		assertTrue("warning should quote the name as typed: " + warnings.get(0),
			warnings.get(0).contains("Typoed Nmae"));
	}

	@Test
	public void noRosterWarningsWhenEveryNameMatches()
	{
		final MemberRules rules = parse("Alt One", "Zezima=2015-03-14");
		final Set<String> roster = names("zezima", "alt one");

		assertEquals(0, ClanTenureService.validateAgainstRoster(rules, roster).size());
	}

	@Test
	public void rosterCheckIsSkippedBeforeTheRosterLoads()
	{
		final MemberRules rules = parse("Alt One", "");

		assertEquals(0, ClanTenureService.validateAgainstRoster(rules, Collections.emptySet()).size());
	}

	private static Set<String> names(String... values)
	{
		return new HashSet<>(Arrays.asList(values));
	}

	private static boolean containing(List<String> warnings, String fragment)
	{
		return warnings.stream().anyMatch(w -> w.contains(fragment));
	}
}
