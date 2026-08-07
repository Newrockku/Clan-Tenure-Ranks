# Clan Tenure Ranks

A RuneLite plugin that reads your clan's roster and shows who is due a rank promotion, based on
how long each member has been in the clan.

It reads the same data the in-game **Clan Settings > Members** list shows — name, rank, and joined
date — for every member, online or not, and compares each member's tenure against a rank ladder
you configure.

**It is read-only.** RuneLite has no API to change ranks, and automating that would breach Jagex's
third-party client rules. The plugin tells you who to promote; you promote them yourself.

## Configuring the ladder

Two settings, under **Clan Tenure Ranks** in the RuneLite config panel. Both accept commas or new
lines, ignore case and extra spaces, and skip lines starting with `#` or `//`.

### Eligible ranks

The tenure ladder, **lowest rank first**:

```
Jade, Red Topaz, Sapphire, Emerald, Ruby, Diamond, Dragonstone, Onyx, Zenyte
```

This does two jobs:

- **It decides who is tracked.** A member whose current rank is not on this list is ignored
  entirely, so Owner, Deputy Owner, Administrator and any event or staff ranks are left alone
  without needing to be listed anywhere.
- **It sets the order.** Rank order comes from this list, not from the in-game rank numbers.

The first rank listed is the **entry rank** and needs no threshold — it applies from day 0.

### Tenure thresholds

Days required for each rank:

```
120=Red Topaz
240=Sapphire
365=Emerald
485=Ruby
605=Diamond
730=Dragonstone
910=Onyx
1095=Zenyte
```

`Red Topaz=120` works too. Every name here must also appear in Eligible ranks.

## Member exceptions

### Manual join dates

Overrides the join date the game reports, one per line:

```
Zezima=2015-03-14
Some Player=2018-11-02
```

Use it when the in-game date is wrong — someone who left and rejoined, or who was in the clan
before it was remade. The date you set replaces the in-game one, and the member's tenure is
counted from it. Overridden rows are marked `(manual)` in the panel and flagged in the CSV, so
it's always visible that a date was set by hand rather than read from the game.

Dates must be `YYYY-MM-DD` (`2015/03/14` also works). Ambiguous formats like `14/03/2015` are
**rejected with a warning** rather than guessed at, since silently reading a date the wrong way
round would quietly produce wrong tenures.

### Ignored members

Members left out of the report entirely:

```
Alt One, Alt Two
```

They are never counted and never appear as due a promotion. Pick **Ignored** in the Show dropdown
to review who's on the list.

Names in both fields are matched ignoring case and spacing, and any name matching nobody in the
clan is reported as a warning — a typo here would otherwise silently do nothing.

## Notifications

**Notify when someone becomes due**, under Notifications in the config, shows a RuneLite
notification (tray popup, sound, etc. — however your RuneLite-wide notification settings are set
up) whenever a member newly qualifies for a promotion. It's on by default; turn it off there if you
don't want it.

It only fires for members who *became* due since the last refresh, not everyone who happens to be
due — so opening the client for the first time with 20 people already overdue produces silence,
not 20 popups. A handful of new promotions in one refresh get named individually; a larger batch
(for example after editing thresholds) collapses into a single "check the panel" notification
instead of a flood.

## Statuses

| Status | Meaning |
|---|---|
| **Due promotion** | Rank is lower than tenure earns. These are the ones to action. |
| **Correct** | Rank matches tenure exactly. |
| **Over-ranked** | Rank is higher than tenure earns — promoted early, or by mistake. |
| **Not tracked** | Current rank is not on the ladder (staff, event ranks). |
| **Ignored** | Listed under ignored members. |
| **No join date** | The roster gave no join date, so tenure is unknown. |

Tenure is counted in whole days from the join date to today in **UTC**, since join dates are
server-side and using local time would cause off-by-one errors.

## Misconfiguration warnings

A silently mis-parsed ladder would produce confident but wrong promotion advice, so the panel
warns about: threshold names missing from the eligible list, rank names that match no real title
in your clan (catches typos), ranks that need no more time than the rank below them, duplicates,
and unparseable lines.

## Using the panel

Open the **Clan Tenure Ranks** panel from the sidebar. It defaults to showing only members due a
promotion; the **Show** dropdown switches to over-ranked, all tracked, or everyone. **Copy CSV**
puts the rows currently listed on the clipboard for pasting into Discord or a sheet.

---

Building the plugin from source, the roadmap, and Plugin Hub submission notes live in
[CONTRIBUTING.md](CONTRIBUTING.md).
