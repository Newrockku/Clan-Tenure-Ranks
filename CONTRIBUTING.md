# Contributing

## Building and running

Requires a JDK (17 is fine; the plugin itself targets Java 11).

```
./gradlew build     # compile and run the unit tests
./gradlew run       # launch a RuneLite dev client with the plugin loaded
```

To log into the dev client with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Once in game, open the **Clan Tenure Ranks** panel from the sidebar. It defaults to showing only
members due a promotion; the **Show** dropdown switches to over-ranked, all tracked, or everyone.
**Copy CSV** puts the rows currently listed on the clipboard for pasting into Discord or a sheet.

## Not yet built

- Colour-coding the in-game Clan Settings members interface
- An offline snapshot so the panel works while logged out

## Publishing to the Plugin Hub

`runelite-plugin.properties`, `LICENSE` (BSD-2), and `icon.png` are in place. What's left is
external to this repo:

1. Push this repo to a **public** GitHub repository.
2. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub), and add a file at
   `plugins/clan-tenure-ranks` containing:
   ```
   repository=<https url to this repo>.git
   commit=<40-character commit hash to build>
   ```
3. Open a PR against `runelite/plugin-hub` with that one file, describing what the plugin does.
4. CI builds it and a maintainer reviews it against the
   [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
   before merging. Once merged it appears in the in-client Plugin Hub, typically within a day.

See the [Plugin Hub guide](https://github.com/runelite/plugin-hub/blob/master/README.md) for
full details.

Whenever you push new commits after the PR is open (or after it's merged, for an update PR),
remember to bump the `commit=` line in `plugins/clan-tenure-ranks` in your plugin-hub fork/PR to
match — the Hub only ever builds the exact commit pinned there.
