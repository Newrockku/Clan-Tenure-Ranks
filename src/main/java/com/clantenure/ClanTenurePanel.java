package com.clantenure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel listing clan members and how their rank compares to their tenure.
 */
class ClanTenurePanel extends PluginPanel
{
	/**
	 * Rendering hundreds of rows is pointless in a 225px sidebar; the CSV export covers bulk review.
	 */
	private static final int MAX_ROWS = 300;

	private enum Filter
	{
		DUE("Due promotion", TenureStatus.DUE_PROMOTION),
		OVER("Over-ranked", TenureStatus.OVER_RANKED),
		TRACKED("All tracked", null),
		IGNORED("Ignored", TenureStatus.IGNORED),
		EVERYONE("Everyone", null);

		private final String label;
		private final TenureStatus only;

		Filter(String label, TenureStatus only)
		{
			this.label = label;
			this.only = only;
		}

		boolean accepts(MemberTenure member)
		{
			if (only != null)
			{
				return member.getStatus() == only;
			}
			if (this == EVERYONE)
			{
				return true;
			}
			return member.getStatus() != TenureStatus.NOT_TRACKED && member.getStatus() != TenureStatus.IGNORED;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private enum Sort
	{
		LONGEST("Longest tenure"),
		SHORTEST("Shortest tenure"),
		NAME("Name (A-Z)");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final JLabel summaryLabel = new JLabel();
	private final JPanel warningPanel = new JPanel();
	private final JPanel listPanel = new JPanel(new GridLayout(0, 1, 0, 3));
	private final JComboBox<Filter> filterBox = new JComboBox<>(Filter.values());
	private final JComboBox<Sort> sortBox = new JComboBox<>(Sort.values());

	private ClanSnapshot snapshot = ClanSnapshot.empty(ClanSnapshot.State.LOGGED_OUT, Collections.emptyList());
	private List<MemberTenure> visible = Collections.emptyList();

	ClanTenurePanel(Runnable refreshAction)
	{
		super(true);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout(0, 8));

		add(buildHeader(refreshAction), BorderLayout.NORTH);

		final JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(listPanel, BorderLayout.NORTH);
		add(listWrapper, BorderLayout.CENTER);

		render();
	}

	private JPanel buildHeader(Runnable refreshAction)
	{
		final JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel title = new JLabel("Clan Tenure Ranks");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		header.add(title);

		summaryLabel.setFont(FontManager.getRunescapeSmallFont());
		summaryLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
		summaryLabel.setBorder(new EmptyBorder(4, 0, 6, 0));
		header.add(summaryLabel);

		warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.Y_AXIS));
		warningPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		warningPanel.setAlignmentX(LEFT_ALIGNMENT);
		header.add(warningPanel);

		filterBox.setSelectedItem(Filter.DUE);
		filterBox.setFocusable(false);
		filterBox.addActionListener(e -> render());
		header.add(labelled("Show", filterBox));

		sortBox.setSelectedItem(Sort.LONGEST);
		sortBox.setFocusable(false);
		sortBox.addActionListener(e -> render());
		header.add(labelled("Sort", sortBox));

		final JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.setBorder(new EmptyBorder(6, 0, 0, 0));
		buttons.setAlignmentX(LEFT_ALIGNMENT);

		final JButton refreshButton = new JButton("Refresh");
		refreshButton.setFocusable(false);
		refreshButton.setToolTipText("Re-read the clan roster from the client");
		refreshButton.addActionListener(e -> refreshAction.run());
		buttons.add(refreshButton);

		final JButton copyButton = new JButton("Copy CSV");
		copyButton.setFocusable(false);
		copyButton.setToolTipText("Copy the rows shown below to the clipboard as CSV");
		copyButton.addActionListener(e -> copyCsv());
		buttons.add(copyButton);

		header.add(buttons);
		return header;
	}

	private JPanel labelled(String text, JComboBox<?> box)
	{
		final JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(2, 0, 2, 0));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setPreferredSize(new Dimension(34, 0));

		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);
		return row;
	}

	void display(ClanSnapshot snapshot)
	{
		this.snapshot = snapshot;
		render();
	}

	private void render()
	{
		final Filter filter = (Filter) filterBox.getSelectedItem();
		final Sort sort = (Sort) sortBox.getSelectedItem();

		visible = snapshot.getMembers().stream()
			.filter(m -> filter == null || filter.accepts(m))
			.sorted(comparator(sort))
			.collect(Collectors.toList());

		summaryLabel.setText(buildSummary());

		warningPanel.removeAll();
		for (String warning : snapshot.getWarnings())
		{
			warningPanel.add(buildWarning(warning));
		}

		listPanel.removeAll();
		if (visible.isEmpty())
		{
			listPanel.add(buildMessage(emptyMessage(filter)));
		}
		else
		{
			for (MemberTenure member : visible.subList(0, Math.min(visible.size(), MAX_ROWS)))
			{
				listPanel.add(buildRow(member));
			}
			if (visible.size() > MAX_ROWS)
			{
				listPanel.add(buildMessage("Showing the first " + MAX_ROWS + " of " + visible.size()
					+ ". Use Copy CSV for the full list."));
			}
		}

		revalidate();
		repaint();
	}

	private static Comparator<MemberTenure> comparator(Sort sort)
	{
		if (sort == Sort.NAME)
		{
			return Comparator.comparing(m -> m.getName().toLowerCase(java.util.Locale.ROOT));
		}
		final Comparator<MemberTenure> byDays = Comparator.comparingLong(MemberTenure::getTenureDays);
		return sort == Sort.SHORTEST ? byDays : byDays.reversed();
	}

	private String buildSummary()
	{
		switch (snapshot.getState())
		{
			case LOGGED_OUT:
				return "Not logged in.";
			case NO_CLAN:
				return "You are not in a clan.";
			case NO_LADDER:
				return "No eligible ranks configured.";
			case NO_JOIN_DATES:
				return "Join dates not loaded yet.";
			default:
				break;
		}

		final String clan = snapshot.getClanName() == null ? "Clan" : snapshot.getClanName();
		final long ignored = snapshot.count(TenureStatus.IGNORED);
		return "<html>" + escape(clan) + "<br>" + snapshot.trackedCount() + " tracked, "
			+ snapshot.count(TenureStatus.DUE_PROMOTION) + " due, "
			+ snapshot.count(TenureStatus.OVER_RANKED) + " over-ranked"
			+ (ignored > 0 ? "<br>" + ignored + " ignored" : "") + "</html>";
	}

	private String emptyMessage(Filter filter)
	{
		switch (snapshot.getState())
		{
			case LOGGED_OUT:
				return "Log in to load your clan roster.";
			case NO_CLAN:
				return "This account is not in a clan.";
			case NO_LADDER:
				return "Add your rank ladder under Eligible ranks in the plugin config.";
			case NO_JOIN_DATES:
				return "The roster loaded without join dates. Open the Clan tab in-game, then press Refresh.";
			default:
				break;
		}

		if (filter == Filter.DUE)
		{
			return "Nobody is due a promotion.";
		}
		if (filter == Filter.OVER)
		{
			return "Nobody is over-ranked.";
		}
		if (filter == Filter.IGNORED)
		{
			return "No ignored members. Add names under Member exceptions in the plugin config.";
		}
		return "No members match this filter.";
	}

	private JPanel buildRow(MemberTenure member)
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 7, 5, 7));

		final JLabel name = new JLabel(member.getName());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		name.setAlignmentX(LEFT_ALIGNMENT);
		row.add(name);

		final JLabel tenure = new JLabel(tenureText(member));
		tenure.setFont(FontManager.getRunescapeSmallFont());
		tenure.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		tenure.setAlignmentX(LEFT_ALIGNMENT);
		row.add(tenure);

		final JLabel rank = new JLabel(rankText(member));
		rank.setFont(FontManager.getRunescapeSmallFont());
		rank.setForeground(member.getStatus().getColor());
		rank.setAlignmentX(LEFT_ALIGNMENT);
		row.add(rank);

		return row;
	}

	private static String tenureText(MemberTenure member)
	{
		if (member.getJoinDate() == null)
		{
			return "join date unknown";
		}
		// Ignored members are not evaluated, so their day count is meaningless - show only the date.
		if (member.getStatus() == TenureStatus.IGNORED)
		{
			return "joined " + member.getJoinDate();
		}
		return member.getTenureDays() + "d  -  joined " + member.getJoinDate()
			+ (member.isJoinDateOverridden() ? "  (manual)" : "");
	}

	private static String rankText(MemberTenure member)
	{
		final String current = member.getCurrentRank() == null ? "no rank" : member.getCurrentRank();
		switch (member.getStatus())
		{
			case DUE_PROMOTION:
				return current + " -> " + member.getEarnedRank();
			case OVER_RANKED:
				return current + "  (earns " + member.getEarnedRank() + ")";
			case NOT_TRACKED:
				return current + "  (not tracked)";
			case IGNORED:
				return current + "  (ignored)";
			case UNKNOWN:
				return current;
			default:
				return current;
		}
	}

	private JPanel buildWarning(String text)
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(new Color(70, 50, 20));
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(4, 6, 4, 6)));
		panel.setAlignmentX(LEFT_ALIGNMENT);

		final JLabel label = new JLabel("<html><body style='width:170px'>" + escape(text) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(new Color(255, 190, 100));
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildMessage(String text)
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(10, 8, 10, 8));

		final JLabel label = new JLabel("<html><body style='width:170px'>" + escape(text) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}

	private void copyCsv()
	{
		final StringBuilder csv = new StringBuilder("Name,Joined,Manual date,Days,Current rank,Earned rank,Status\n");
		for (MemberTenure member : visible)
		{
			csv.append(quote(member.getName())).append(',')
				.append(member.getJoinDate() == null ? "" : member.getJoinDate().toString()).append(',')
				.append(member.isJoinDateOverridden() ? "yes" : "").append(',')
				.append(member.getJoinDate() == null ? "" : Long.toString(member.getTenureDays())).append(',')
				.append(quote(member.getCurrentRank() == null ? "" : member.getCurrentRank())).append(',')
				.append(quote(member.getEarnedRank() == null ? "" : member.getEarnedRank())).append(',')
				.append(quote(member.getStatus().getLabel())).append('\n');
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(csv.toString()), null);
	}

	private static String quote(String value)
	{
		if (value.indexOf(',') < 0 && value.indexOf('"') < 0)
		{
			return value;
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 * Exposed for the CSV button and tests: the rows currently shown, after filter and sort.
	 */
	List<MemberTenure> getVisible()
	{
		return new ArrayList<>(visible);
	}
}
