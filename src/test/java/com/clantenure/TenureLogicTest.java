package com.clantenure;

import java.time.LocalDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Drives {@link ClanTenureService#evaluate} directly, so the promotion rules are proven without a
 * running game client.
 */
public class TenureLogicTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 6);

	private static final RankLadder LADDER =
		RankLadder.parse(ClanTenureConfig.DEFAULT_ELIGIBLE_RANKS, ClanTenureConfig.DEFAULT_THRESHOLDS);

	private static MemberTenure evaluate(String currentRank, long daysInClan)
	{
		return ClanTenureService.evaluate("Player", TODAY.minusDays(daysInClan), currentRank, LADDER, TODAY);
	}

	@Test
	public void memberAtTheRightRankIsCorrect()
	{
		final MemberTenure member = evaluate("Emerald", 400);

		assertEquals(TenureStatus.CORRECT, member.getStatus());
		assertEquals("Emerald", member.getEarnedRank());
		assertEquals(400, member.getTenureDays());
	}

	@Test
	public void memberBehindTheirTenureIsDueAPromotion()
	{
		final MemberTenure member = evaluate("Onyx", 1204);

		assertEquals(TenureStatus.DUE_PROMOTION, member.getStatus());
		assertEquals("Zenyte", member.getEarnedRank());
	}

	@Test
	public void memberAheadOfTheirTenureIsOverRanked()
	{
		final MemberTenure member = evaluate("Onyx", 700);

		assertEquals(TenureStatus.OVER_RANKED, member.getStatus());
		assertEquals("Diamond", member.getEarnedRank());
	}

	@Test
	public void thresholdBoundaryIsInclusiveOnTheDayItIsReached()
	{
		assertEquals(TenureStatus.CORRECT, evaluate("Jade", 119).getStatus());
		assertEquals(TenureStatus.DUE_PROMOTION, evaluate("Jade", 120).getStatus());
		assertEquals("Red Topaz", evaluate("Jade", 120).getEarnedRank());
		assertEquals("Red Topaz", evaluate("Jade", 121).getEarnedRank());
	}

	@Test
	public void topOfTheLadderStaysAtTheTop()
	{
		final MemberTenure member = evaluate("Zenyte", 4000);

		assertEquals(TenureStatus.CORRECT, member.getStatus());
		assertEquals("Zenyte", member.getEarnedRank());
	}

	@Test
	public void brandNewMemberFloorsAtTheEntryRank()
	{
		final MemberTenure member = evaluate("Jade", 0);

		assertEquals(TenureStatus.CORRECT, member.getStatus());
		assertEquals("Jade", member.getEarnedRank());
		assertEquals(0, member.getTenureDays());
	}

	@Test
	public void offLadderRanksAreLeftAlone()
	{
		for (String staffRank : new String[]{"Owner", "Deputy Owner", "Administrator", "Event Team"})
		{
			final MemberTenure member = evaluate(staffRank, 4000);

			assertEquals(staffRank + " should not be tracked", TenureStatus.NOT_TRACKED, member.getStatus());
			assertNull(member.getEarnedRank());
		}
	}

	@Test
	public void memberWithNoRankTitleIsNotTracked()
	{
		final MemberTenure member = ClanTenureService.evaluate("Player", TODAY.minusDays(500), null, LADDER, TODAY);

		assertEquals(TenureStatus.NOT_TRACKED, member.getStatus());
	}

	@Test
	public void missingJoinDateOnALadderRankIsReportedRatherThanGuessed()
	{
		final MemberTenure member = ClanTenureService.evaluate("Player", null, "Emerald", LADDER, TODAY);

		assertEquals(TenureStatus.UNKNOWN, member.getStatus());
		assertEquals(0, member.getTenureDays());
		assertNull(member.getEarnedRank());
		assertNull(member.getJoinDate());
	}

	@Test
	public void offLadderRankWinsOverAMissingJoinDate()
	{
		final MemberTenure member = ClanTenureService.evaluate("Player", null, "Owner", LADDER, TODAY);

		assertEquals(TenureStatus.NOT_TRACKED, member.getStatus());
	}

	@Test
	public void rankNameFromTheClientIsMatchedCaseInsensitively()
	{
		final MemberTenure member = evaluate("dragonstone", 800);

		assertEquals(TenureStatus.CORRECT, member.getStatus());
	}

	/**
	 * The point of a manual join date: backdating someone makes them due the rank their real tenure
	 * has earned, even though the game reports them as newer.
	 */
	@Test
	public void olderManualJoinDatePromotesAMemberTheGameDateWouldNot()
	{
		final LocalDate gameDate = TODAY.minusDays(100);
		final LocalDate backdated = TODAY.minusDays(400);

		final MemberTenure fromGame = ClanTenureService.evaluate("Player", gameDate, "Jade", LADDER, TODAY);
		assertEquals(TenureStatus.CORRECT, fromGame.getStatus());

		final MemberTenure overridden =
			ClanTenureService.evaluate("Player", backdated, "Jade", LADDER, TODAY, true);

		assertEquals(TenureStatus.DUE_PROMOTION, overridden.getStatus());
		assertEquals("Emerald", overridden.getEarnedRank());
		assertEquals(400, overridden.getTenureDays());
		assertTrue(overridden.isJoinDateOverridden());
	}

	@Test
	public void joinDatesFromTheGameAreNotMarkedAsManual()
	{
		assertFalse(evaluate("Emerald", 400).isJoinDateOverridden());
	}

	@Test
	public void futureJoinDateClampsToZeroInsteadOfGoingNegative()
	{
		final MemberTenure member = ClanTenureService.evaluate("Player", TODAY.plusDays(30), "Jade", LADDER, TODAY);

		assertEquals(0, member.getTenureDays());
		assertEquals(TenureStatus.CORRECT, member.getStatus());
	}
}
