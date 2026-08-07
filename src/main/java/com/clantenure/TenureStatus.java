package com.clantenure;

import java.awt.Color;
import lombok.Getter;

/**
 * How a member's current clan rank compares to the rank their tenure has earned them.
 */
@Getter
public enum TenureStatus
{
	/**
	 * The member holds a lower rank than their tenure earns. These are the promotions to action.
	 */
	DUE_PROMOTION("Due promotion", new Color(76, 175, 80)),
	/**
	 * The member's rank matches their tenure exactly.
	 */
	CORRECT("Correct", new Color(150, 150, 150)),
	/**
	 * The member holds a higher rank than their tenure earns - promoted early, or by mistake.
	 */
	OVER_RANKED("Over-ranked", new Color(255, 152, 0)),
	/**
	 * The member's current rank is not on the eligible-ranks ladder, so tenure does not apply to
	 * them. Staff and event ranks land here.
	 */
	NOT_TRACKED("Not tracked", new Color(110, 110, 110)),
	/**
	 * The clan roster gave us no join date for this member, so tenure cannot be calculated.
	 */
	UNKNOWN("No join date", new Color(200, 80, 80)),
	/**
	 * Listed under ignored members, so left out of every count and filter.
	 */
	IGNORED("Ignored", new Color(110, 110, 110));

	private final String label;
	private final Color color;

	TenureStatus(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}
}
