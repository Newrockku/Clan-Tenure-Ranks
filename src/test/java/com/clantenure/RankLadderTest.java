package com.clantenure;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RankLadderTest
{
	private static RankLadder defaultLadder()
	{
		return RankLadder.parse(ClanTenureConfig.DEFAULT_ELIGIBLE_RANKS, ClanTenureConfig.DEFAULT_THRESHOLDS);
	}

	@Test
	public void parsesTheShippedDefaultsCleanly()
	{
		final RankLadder ladder = defaultLadder();

		assertEquals("no warnings expected for the default config: " + ladder.getWarnings(),
			0, ladder.getWarnings().size());
		assertEquals(9, ladder.size());
		assertEquals("Jade", ladder.nameAt(0));
		assertEquals("Zenyte", ladder.nameAt(8));
	}

	@Test
	public void entryRankSitsAtZeroDaysAndThresholdsMatchConfig()
	{
		final RankLadder ladder = defaultLadder();

		assertEquals(0, ladder.thresholdAt(0));
		assertEquals(120, ladder.thresholdAt(1));
		assertEquals(240, ladder.thresholdAt(2));
		assertEquals(365, ladder.thresholdAt(3));
		assertEquals(485, ladder.thresholdAt(4));
		assertEquals(605, ladder.thresholdAt(5));
		assertEquals(730, ladder.thresholdAt(6));
		assertEquals(910, ladder.thresholdAt(7));
		assertEquals(1095, ladder.thresholdAt(8));
	}

	@Test
	public void rankNamesMatchIgnoringCaseAndExtraWhitespace()
	{
		final RankLadder ladder = defaultLadder();

		assertEquals(1, ladder.indexOf("red topaz").getAsInt());
		assertEquals(1, ladder.indexOf("  RED   TOPAZ ").getAsInt());
		assertFalse(ladder.indexOf("Deputy Owner").isPresent());
		assertFalse(ladder.indexOf(null).isPresent());
	}

	@Test
	public void acceptsCommasNewlinesAndComments()
	{
		final RankLadder ladder = RankLadder.parse(
			"# the ladder\nJade\nRed Topaz, Sapphire\n",
			"// thresholds\n120=Red Topaz\n240=Sapphire");

		assertEquals(3, ladder.size());
		assertEquals(120, ladder.thresholdAt(1));
		assertEquals(240, ladder.thresholdAt(2));
		assertEquals(0, ladder.getWarnings().size());
	}

	@Test
	public void acceptsNameEqualsDaysAsWellAsDaysEqualsName()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "Sapphire = 240");

		assertEquals(240, ladder.thresholdAt(1));
		assertEquals(0, ladder.getWarnings().size());
	}

	@Test
	public void warnsWhenAThresholdNamesARankThatIsNotEligible()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "120=Red Topaz\n240=Sapphire");

		assertTrue(containing(ladder.getWarnings(), "not in your eligible ranks"));
	}

	@Test
	public void warnsOnDuplicateEligibleRankAndKeepsOneRung()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire, sapphire", "240=Sapphire");

		assertEquals(2, ladder.size());
		assertTrue(containing(ladder.getWarnings(), "Duplicate eligible rank"));
	}

	@Test
	public void warnsOnMalformedThresholdLines()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "Sapphire\n=\nabc=Sapphire");

		assertTrue(containing(ladder.getWarnings(), "expected the form"));
		assertTrue(containing(ladder.getWarnings(), "neither side is a number"));
	}

	@Test
	public void warnsWhenAMidLadderRankHasNoThreshold()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Red Topaz, Sapphire", "240=Sapphire");

		assertTrue(containing(ladder.getWarnings(), "\"Red Topaz\" has no tenure threshold"));
	}

	@Test
	public void warnsWhenAHigherRankNeedsNoMoreTimeThanALowerOne()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Red Topaz, Sapphire", "240=Red Topaz\n120=Sapphire");

		assertTrue(containing(ladder.getWarnings(), "requires no more time than"));
	}

	@Test
	public void warnsWhenTheEntryRankIsGivenAThreshold()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "30=Jade\n240=Sapphire");

		assertTrue(containing(ladder.getWarnings(), "treated as the entry rank"));
		assertEquals(0, ladder.thresholdAt(0));
	}

	@Test
	public void warnsOnDuplicateThresholdAndKeepsTheFirst()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "240=Sapphire\n999=Sapphire");

		assertEquals(240, ladder.thresholdAt(1));
		assertTrue(containing(ladder.getWarnings(), "Duplicate threshold"));
	}

	@Test
	public void clampsNegativeThresholdsToZero()
	{
		final RankLadder ladder = RankLadder.parse("Jade, Sapphire", "-5=Sapphire");

		assertEquals(0, ladder.thresholdAt(1));
		assertTrue(containing(ladder.getWarnings(), "negative"));
	}

	@Test
	public void emptyConfigIsUnusableRatherThanCrashing()
	{
		final RankLadder ladder = RankLadder.parse("", "");

		assertFalse(ladder.isUsable());
		assertTrue(containing(ladder.getWarnings(), "No eligible ranks"));
	}

	@Test
	public void nullConfigIsTreatedAsEmpty()
	{
		final RankLadder ladder = RankLadder.parse(null, null);

		assertFalse(ladder.isUsable());
	}

	@Test
	public void earnedIndexPicksTheHighestRankReached()
	{
		final RankLadder ladder = defaultLadder();

		assertEquals(0, ladder.earnedIndex(0));
		assertEquals(0, ladder.earnedIndex(119));
		assertEquals(1, ladder.earnedIndex(120));
		assertEquals(1, ladder.earnedIndex(239));
		assertEquals(2, ladder.earnedIndex(240));
		assertEquals(8, ladder.earnedIndex(1095));
		assertEquals(8, ladder.earnedIndex(50_000));
	}

	private static boolean containing(List<String> warnings, String fragment)
	{
		return warnings.stream().anyMatch(w -> w.contains(fragment));
	}
}
