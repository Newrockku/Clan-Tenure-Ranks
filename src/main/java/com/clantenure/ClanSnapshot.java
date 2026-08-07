package com.clantenure;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * Everything the panel needs for one render: why there is or isn't data, the evaluated roster, and
 * any config problems worth showing the user.
 */
@Value
public class ClanSnapshot
{
	public enum State
	{
		/** Not logged in, so the client holds no clan data. */
		LOGGED_OUT,
		/** Logged in, but the account is not in a clan. */
		NO_CLAN,
		/** Roster present but every member is missing a join date. */
		NO_JOIN_DATES,
		/** The eligible-ranks config is empty, so nothing can be compared. */
		NO_LADDER,
		/** Usable data. */
		OK
	}

	State state;

	@Nullable
	String clanName;

	List<MemberTenure> members;

	/**
	 * Config problems (bad thresholds, names that match no real clan title, ordering mistakes).
	 */
	List<String> warnings;

	public static ClanSnapshot empty(State state, List<String> warnings)
	{
		return new ClanSnapshot(state, null, Collections.emptyList(), warnings);
	}

	public long count(TenureStatus status)
	{
		return members.stream().filter(m -> m.getStatus() == status).count();
	}

	/**
	 * Members the ladder actually applies to - excluding off-ladder ranks and ignored members.
	 */
	public long trackedCount()
	{
		return members.stream()
			.filter(m -> m.getStatus() != TenureStatus.NOT_TRACKED && m.getStatus() != TenureStatus.IGNORED)
			.count();
	}
}
