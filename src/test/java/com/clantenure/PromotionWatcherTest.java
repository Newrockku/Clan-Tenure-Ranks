package com.clantenure;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PromotionWatcherTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 6);
	private static final RankLadder LADDER =
		RankLadder.parse(ClanTenureConfig.DEFAULT_ELIGIBLE_RANKS, ClanTenureConfig.DEFAULT_THRESHOLDS);

	private PromotionWatcher watcher;

	@Before
	public void setUp()
	{
		watcher = new PromotionWatcher();
	}

	private static MemberTenure due(String name)
	{
		return ClanTenureService.evaluate(name, TODAY.minusDays(1204), "Onyx", LADDER, TODAY);
	}

	private static MemberTenure correct(String name)
	{
		return ClanTenureService.evaluate(name, TODAY.minusDays(400), "Emerald", LADDER, TODAY);
	}

	private static ClanSnapshot snapshot(String clanName, MemberTenure... members)
	{
		return new ClanSnapshot(ClanSnapshot.State.OK, clanName, Arrays.asList(members), Collections.emptyList());
	}

	@Test
	public void firstSuccessfulRefreshSeedsWithoutReportingAnybody()
	{
		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("Zezima"), due("Woox")));

		assertTrue("first refresh should not flood notifications for an already-overdue roster",
			newlyDue.isEmpty());
	}

	@Test
	public void memberBecomingDueAfterASeededRefreshIsReported()
	{
		watcher.update(snapshot("My Clan", correct("Zezima")));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("Zezima")));

		assertEquals(1, newlyDue.size());
		assertEquals("Zezima", newlyDue.get(0).getName());
	}

	@Test
	public void memberStayingDueAcrossRefreshesIsNotReportedAgain()
	{
		watcher.update(snapshot("My Clan", due("Zezima")));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("Zezima")));

		assertTrue(newlyDue.isEmpty());
	}

	@Test
	public void memberWhoDropsOffAndBecomesDueAgainIsReportedTheSecondTime()
	{
		watcher.update(snapshot("My Clan", due("Zezima")));
		watcher.update(snapshot("My Clan", correct("Zezima")));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("Zezima")));

		assertEquals(1, newlyDue.size());
	}

	@Test
	public void switchingToADifferentClanReseedsWithoutReporting()
	{
		watcher.update(snapshot("My Clan", correct("Zezima")));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("Other Clan", due("Woox")));

		assertTrue("a different clan has no baseline to compare against yet", newlyDue.isEmpty());
	}

	@Test
	public void nonOkSnapshotsAreIgnoredAndDoNotResetTheBaseline()
	{
		watcher.update(snapshot("My Clan", correct("Zezima")));

		watcher.update(ClanSnapshot.empty(ClanSnapshot.State.LOGGED_OUT, Collections.emptyList()));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("Zezima")));

		assertEquals("a logout blip in between should not have cleared the baseline", 1, newlyDue.size());
	}

	@Test
	public void nameMatchingIgnoresCaseSoAConfigTypoDoesNotDoubleReport()
	{
		watcher.update(snapshot("My Clan", correct("Zezima")));
		watcher.update(snapshot("My Clan", due("Zezima")));

		final List<MemberTenure> newlyDue = watcher.update(snapshot("My Clan", due("zezima")));

		assertTrue(newlyDue.isEmpty());
	}

	@Test
	public void singleMemberMessageNamesThemAndTheirEarnedRank()
	{
		final String message = PromotionWatcher.message(Collections.singletonList(due("Zezima")));

		assertEquals("Zezima is now due a promotion to Zenyte.", message);
	}

	@Test
	public void smallBatchMessageListsEveryName()
	{
		final String message = PromotionWatcher.message(Arrays.asList(due("Zezima"), due("Woox")));

		assertEquals("2 members are now due a promotion: Zezima, Woox", message);
	}

	@Test
	public void largeBatchMessageCollapsesToACountInsteadOfListingEveryName()
	{
		final List<MemberTenure> many = Arrays.asList(
			due("A"), due("B"), due("C"), due("D"), due("E"), due("F"));

		final String message = PromotionWatcher.message(many);

		assertEquals("6 members are now due a promotion - check the Clan Tenure Ranks panel.", message);
	}
}
