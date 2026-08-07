package com.clantenure;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Clan Tenure Ranks",
	description = "Shows which clan members are due a rank promotion based on how long they have been in the clan",
	tags = {"clan", "rank", "tenure", "promotion", "members"}
)
public class ClanTenurePlugin extends Plugin
{
	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClanTenureConfig config;

	@Inject
	private ClanTenureService service;

	@Inject
	private Notifier notifier;

	private ClanTenurePanel panel;
	private NavigationButton navButton;
	private PromotionWatcher promotionWatcher;

	@Override
	protected void startUp()
	{
		panel = new ClanTenurePanel(this::refresh);
		promotionWatcher = new PromotionWatcher();

		final BufferedImage icon = ImageUtil.loadImageResource(ClanTenurePlugin.class, "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Clan Tenure Ranks")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		refresh();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		promotionWatcher = null;
	}

	/**
	 * The roster arrives shortly after login, and this is the event that tells us it is ready.
	 */
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (!event.isGuest())
		{
			refresh();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN || state == GameState.LOGIN_SCREEN)
		{
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClanTenureConfig.GROUP.equals(event.getGroup()))
		{
			refresh();
		}
	}

	/**
	 * Parses the ladder off the calling thread, reads the roster on the client thread, then hands the
	 * finished snapshot to Swing.
	 */
	void refresh()
	{
		final ClanTenurePanel target = panel;
		if (target == null)
		{
			return;
		}

		final RankLadder ladder = RankLadder.parse(config.eligibleRanks(), config.tenureThresholds());
		final MemberRules rules = MemberRules.parse(config.ignoredMembers(), config.joinDateOverrides(),
			LocalDate.now(ZoneOffset.UTC));
		final boolean notifyEnabled = config.notifyOnPromotion();

		clientThread.invoke(() ->
		{
			final ClanSnapshot snapshot = service.snapshot(ladder, rules);

			// promotionWatcher tracks the previous refresh regardless of the toggle, so switching
			// notifications on does not immediately dump a backlog of everyone already due.
			final PromotionWatcher watcher = promotionWatcher;
			if (watcher != null)
			{
				final List<MemberTenure> newlyDue = watcher.update(snapshot);
				if (notifyEnabled && !newlyDue.isEmpty())
				{
					notifier.notify(PromotionWatcher.message(newlyDue));
				}
			}

			SwingUtilities.invokeLater(() -> target.display(snapshot));
		});
	}

	@Provides
	ClanTenureConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanTenureConfig.class);
	}
}
