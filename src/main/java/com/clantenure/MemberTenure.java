package com.clantenure;

import java.time.LocalDate;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One clan member, evaluated against the tenure ladder.
 */
@Value
public class MemberTenure
{
	String name;

	/**
	 * Null when the roster gave us no join date, in which case the status is
	 * {@link TenureStatus#UNKNOWN} and the tenure is 0.
	 */
	@Nullable
	LocalDate joinDate;

	long tenureDays;

	/**
	 * The member's current clan title, or null if their rank has no title configured in-game.
	 */
	@Nullable
	String currentRank;

	/**
	 * The rank their tenure earns. Null when the member is not on the ladder, or has no join date.
	 */
	@Nullable
	String earnedRank;

	TenureStatus status;

	/**
	 * True when {@link #joinDate} came from the manual join date config rather than the game.
	 */
	boolean joinDateOverridden;
}
