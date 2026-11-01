package com.miscellaniamonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import net.runelite.client.ui.PluginPanel;

/** Read-only kingdom status and reference guide in a responsive, stacked ledger layout. */
final class MiscellaniaMonitorPanel extends PluginPanel
{
	static final Color SURFACE = new Color(25, 22, 24);
	static final Color CARD = new Color(35, 29, 32);
	static final Color BURGUNDY = new Color(67, 27, 37);
	static final Color PARCHMENT = new Color(231, 218, 191);
	static final Color MUTED = new Color(173, 155, 129);
	static final Color GOLD = new Color(198, 162, 95);
	static final Color DEEP_GOLD = new Color(111, 82, 44);
	private static final Color BORDER = new Color(72, 49, 43);
	private static final Color LOW_GOLD = new Color(220, 174, 100);
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance();

	private final JLabel state = new JLabel("WAITING FOR CLIENT DATA", SwingConstants.CENTER);
	private final JLabel approval = new JLabel("unknown");
	private final JLabel approvalCondition = new JLabel(" ");
	private final JLabel coffer = new JLabel("unknown");
	private final JLabel wageTarget = new JLabel("unknown");
	private final JTextArea cofferRunway = valueArea("unknown", GOLD, Font.SERIF, 15);
	private final JLabel update = new JLabel("unknown");
	private final JTextArea collectionLog = valueArea("No collections recorded yet", MUTED, Font.SANS_SERIF, 10);
	private final JProgressBar approvalBar = new JProgressBar(0, 100);
	private final Runnable resetCollectionLog;

	MiscellaniaMonitorPanel(Runnable resetCollectionLog)
	{
		this.resetCollectionLog = resetCollectionLog;
		setLayout(new BorderLayout(0, 8));
		setBackground(SURFACE);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setMinimumSize(new Dimension(0, 0));

		JPanel header = new JPanel(new BorderLayout(0, 3));
		header.setBackground(BURGUNDY);
		header.setBorder(BorderFactory.createCompoundBorder(new GothicBorder(),
			BorderFactory.createEmptyBorder(9, 9, 9, 9)));
		JLabel eyebrow = new JLabel("KINGDOM LEDGER  ·  MISCELLANIA", SwingConstants.CENTER);
		eyebrow.setForeground(GOLD);
		eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
		JLabel title = new JLabel("Managing Miscellania", SwingConstants.CENTER);
		title.setForeground(PARCHMENT);
		title.setFont(new Font(Font.SERIF, Font.BOLD, 16));
		header.add(eyebrow, BorderLayout.NORTH);
		header.add(title, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);

		JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(SURFACE);
		tabs.setForeground(PARCHMENT);
		tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		tabs.setFocusable(false);
		tabs.setBorder(BorderFactory.createEmptyBorder());
		tabs.setUI(new LedgerTabbedPaneUI());
		tabs.addTab("Status", createStatusView());
		tabs.addTab("Field Guide", createGuideView());
		add(tabs, BorderLayout.CENTER);
	}

	private JPanel createStatusView()
	{
		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(SURFACE);
		content.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 2));

		state.setForeground(MUTED);
		state.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		state.setBorder(BorderFactory.createEmptyBorder(2, 2, 3, 2));
		approval.setForeground(GOLD);
		coffer.setForeground(GOLD);
		wageTarget.setForeground(PARCHMENT);
		cofferRunway.setForeground(GOLD);
		update.setForeground(PARCHMENT);
		styleValue(approval, true);
		styleValue(coffer, true);
		styleValue(wageTarget, false);
		styleValue(update, false);
		content.add(state, BorderLayout.NORTH);

		JPanel ledger = new JPanel();
		ledger.setLayout(new javax.swing.BoxLayout(ledger, javax.swing.BoxLayout.Y_AXIS));
		ledger.setBackground(SURFACE);
		ledger.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

		JPanel popularityContent = new JPanel();
		popularityContent.setLayout(new javax.swing.BoxLayout(popularityContent, javax.swing.BoxLayout.Y_AXIS));
		popularityContent.setOpaque(false);
		JPanel approvalHeader = new JPanel(new BorderLayout(4, 0));
		approvalHeader.setOpaque(false);
		approvalHeader.add(approval, BorderLayout.CENTER);
		approvalCondition.setHorizontalAlignment(SwingConstants.LEFT);
		approvalCondition.setForeground(MUTED);
		approvalCondition.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
		JPanel approvalStack = new JPanel();
		approvalStack.setLayout(new javax.swing.BoxLayout(approvalStack, javax.swing.BoxLayout.Y_AXIS));
		approvalStack.setOpaque(false);
		approvalStack.add(approvalHeader);
		approvalStack.add(approvalCondition);
		popularityContent.add(approvalStack);
		approvalBar.setStringPainted(false);
		approvalBar.setBorderPainted(false);
		approvalBar.setOpaque(true);
		approvalBar.setBackground(new Color(21, 18, 20));
		approvalBar.setForeground(BURGUNDY);
		approvalBar.setUI(new BasicProgressBarUI());
		approvalBar.setPreferredSize(new Dimension(180, 8));
		approvalBar.setMinimumSize(new Dimension(0, 8));
		popularityContent.add(approvalBar);
		ledger.add(createCard("POPULARITY", popularityContent, true));

		JPanel cofferContent = new JPanel();
		cofferContent.setLayout(new javax.swing.BoxLayout(cofferContent, javax.swing.BoxLayout.Y_AXIS));
		cofferContent.setOpaque(false);
		cofferContent.add(coffer);
		JLabel targetCaption = new JLabel("Daily wage target");
		targetCaption.setForeground(MUTED);
		targetCaption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		targetCaption.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		cofferContent.add(targetCaption);
		cofferContent.add(wageTarget);
		ledger.add(createCard("COFFER BALANCE", cofferContent, true));

		JPanel runwayContent = new JPanel();
		runwayContent.setLayout(new javax.swing.BoxLayout(runwayContent, javax.swing.BoxLayout.Y_AXIS));
		runwayContent.setOpaque(false);
		runwayContent.add(cofferRunway);
		JLabel runwayCaveat = new JLabel("Assumes uninterrupted upkeep");
		runwayCaveat.setForeground(MUTED);
		runwayCaveat.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 9));
		runwayContent.add(runwayCaveat);
		ledger.add(createCard("ESTIMATED COFFER DEPLETION", runwayContent, false));

		JPanel timingContent = new JPanel();
		timingContent.setLayout(new javax.swing.BoxLayout(timingContent, javax.swing.BoxLayout.Y_AXIS));
		timingContent.setOpaque(false);
		JLabel timingCaption = new JLabel("Next UTC update");
		timingCaption.setForeground(MUTED);
		timingCaption.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		timingContent.add(timingCaption);
		timingContent.add(update);
		ledger.add(createCard("KINGDOM CLOCK", timingContent, false));

		JPanel collectionContent = new JPanel(new BorderLayout());
		collectionContent.setOpaque(false);
		collectionLog.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
		collectionContent.add(collectionLog, BorderLayout.CENTER);
		ledger.add(createCard("COLLECTION LOG", collectionContent, false, resetCollectionLog));

		content.add(ledger, BorderLayout.CENTER);
		return content;
	}

	private JPanel createCard(String caption, JPanel body, boolean prominent)
	{
		return createCard(caption, body, prominent, null);
	}

	private JPanel createCard(String caption, JPanel body, boolean prominent, Runnable resetAction)
	{
		JPanel card = new JPanel(new BorderLayout(0, prominent ? 6 : 4));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(new GothicBorder(),
			BorderFactory.createEmptyBorder(8, 9, 8, 9)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setMinimumSize(new Dimension(0, prominent ? 72 : 52));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JLabel heading = new JLabel(caption);
		heading.setForeground(GOLD);
		heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
		if (resetAction == null)
		{
			card.add(heading, BorderLayout.NORTH);
		}
		else
		{
			JButton reset = new JButton("Reset");
			reset.setFocusable(false);
			reset.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
			reset.setForeground(MUTED);
			reset.setBackground(CARD);
			reset.setToolTipText("Clear the local collection log");
			reset.addActionListener(event -> {
				int choice = JOptionPane.showConfirmDialog(this,
					"Clear all entries from the local collection log?",
					"Reset collection log", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (choice == JOptionPane.YES_OPTION)
				{
					resetAction.run();
				}
			});
			JPanel title = new JPanel(new BorderLayout(4, 0));
			title.setOpaque(false);
			title.add(heading, BorderLayout.CENTER);
			title.add(reset, BorderLayout.EAST);
			card.add(title, BorderLayout.NORTH);
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	private JScrollPane createGuideView()
	{
		JEditorPane guide = new JEditorPane("text/html", getGuideHtml());
		guide.setEditable(false);
		guide.setFocusable(false);
		guide.setBackground(SURFACE);
		guide.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane scroll = new JScrollPane(guide);
		styleScrollPane(scroll);
		return scroll;
	}

	private static void styleScrollPane(JScrollPane scroll)
	{
		scroll.setBorder(new GothicBorder());
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setBackground(SURFACE);
		scroll.setBackground(SURFACE);
		scroll.getVerticalScrollBar().setUnitIncrement(12);
	}

	private void styleValue(JLabel label, boolean prominent)
	{
		label.setFont(new Font(prominent ? Font.SERIF : Font.SANS_SERIF,
			prominent ? Font.BOLD : Font.PLAIN, prominent ? 18 : 10));
		label.setMinimumSize(new Dimension(0, label.getPreferredSize().height));
		label.setToolTipText(label.getText());
	}

	void showUnavailable(String message)
	{
		setTextIfChanged(state, message.toUpperCase());
		setForegroundIfChanged(state, MUTED);
		setTextIfChanged(approval, "unknown");
		setTextIfChanged(approvalCondition, " ");
		if (approvalBar.getValue() != 0)
		{
			approvalBar.setValue(0);
		}
		setTextIfChanged(coffer, "unknown");
		setTextIfChanged(wageTarget, "unknown");
		setTextIfChanged(cofferRunway, "unknown");
		setForegroundIfChanged(cofferRunway, MUTED);
		setTextIfChanged(update, "unknown");
		setTextIfChanged(collectionLog, "No collections recorded yet");
		setForegroundIfChanged(collectionLog, MUTED);
	}

	void clearCollectionLog()
	{
		setTextIfChanged(collectionLog, "No collections recorded yet");
		setForegroundIfChanged(collectionLog, MUTED);
	}

	void update(Status status, int approvalWarningThreshold)
	{
		setTextIfChanged(state, status.inKingdom ? "●  IN MISCELLANIA" : "●  KINGDOM STATUS AVAILABLE");
		setForegroundIfChanged(state, status.inKingdom ? GOLD : MUTED);
		setTextIfChanged(approval, status.approvalPercent + "%");
		setForegroundIfChanged(approval,
			status.approvalPercent <= approvalWarningThreshold ? LOW_GOLD : GOLD);
		setTextIfChanged(approvalCondition,
			status.approvalPercent <= approvalWarningThreshold ? "LOW" : "STEADY");
		setForegroundIfChanged(approvalCondition,
			status.approvalPercent <= approvalWarningThreshold ? LOW_GOLD : MUTED);
		if (approvalBar.getValue() != status.approvalPercent)
		{
			approvalBar.setValue(status.approvalPercent);
		}
		setForegroundIfChanged(approvalBar,
			status.approvalPercent <= approvalWarningThreshold ? BURGUNDY : GOLD);
		setTextIfChanged(coffer, NUMBER_FORMAT.format(status.coffer) + " gp");
		setTextIfChanged(wageTarget, NUMBER_FORMAT.format(status.wageTarget) + " gp");
		if (status.coffer < status.wageTarget)
		{
			setTextIfChanged(cofferRunway, "0");
		}
		else
		{
			setTextIfChanged(cofferRunway, NUMBER_FORMAT.format(status.updatesUntilBelowTarget));
		}
		setForegroundIfChanged(cofferRunway, status.coffer < status.wageTarget ? MUTED : GOLD);
		setTextIfChanged(update, status.untilUpdate);
		setForegroundIfChanged(collectionLog, status.collectionLog.isEmpty() ? MUTED : PARCHMENT);
		setTextIfChanged(collectionLog,
			status.collectionLog.isEmpty() ? "No collections recorded yet" : status.collectionLog);
	}

	private static void setTextIfChanged(JLabel label, String text)
	{
		if (!text.equals(label.getText()))
		{
			label.setText(text);
		}
	}

	private static void setTextIfChanged(JTextArea textArea, String text)
	{
		if (!text.equals(textArea.getText()))
		{
			textArea.setText(text);
		}
	}

	private static void setForegroundIfChanged(javax.swing.JComponent component, Color foreground)
	{
		if (!foreground.equals(component.getForeground()))
		{
			component.setForeground(foreground);
		}
	}

	private static String getGuideHtml()
	{
		return "<html><body style='background-color:#191618;color:#e7dabf;font-family:sans-serif;font-size:10pt'>"
			+ "<h3 style='color:#c6a25f'>UNLOCKS</h3>"
			+ "Throne of Miscellania unlocks up to 10 workers. Royal Trouble raises the limit to 15 and increases productivity by 50%."
			+ "<h3 style='color:#c6a25f'>COFFER AND UPKEEP</h3>"
			+ "At each daily update, the coffer pays 10% of its current balance, capped at 50,000 gp (75,000 gp after Royal Trouble). "
			+ "Keeping at least 500,000 gp (750,000 gp after Royal Trouble) supports the full capped wage. The runway estimates "
			+ "updates until the balance falls below that target, assuming upkeep continues without a pause."
			+ "<h3 style='color:#c6a25f'>POPULARITY</h3>"
			+ "Popularity falls by 2.5 percentage points per day before Royal Trouble and 1 point per day after it. Island tasks can raise it to 100%."
			+ "<h3 style='color:#c6a25f'>WORKERS AND ASSIGNMENTS</h3>"
			+ "The six categories are herbs, wood, mining, fishing, hardwood, and farming. Each category allows up to 10 workers; idle workers "
			+ "still incur wages. You can change the split before collecting, and the split in place at collection determines the resource mix. "
			+ "Collection deposits resources directly into your bank."
			+ "<h3 style='color:#c6a25f'>TIMING AND ESTIMATES</h3>"
			+ "Collection and coffer upkeep update at midnight UTC. Gathering and upkeep pause after 30 days away and resume when you return. "
			+ "The plugin does not show exact assignment counts or calculate a yield estimate. It records collection items when the game's "
			+ "visible collection interface exposes them. Entering the island updates popularity and coffer from client values, but it does not open game interfaces. "
			+ "The estimated coffer depletion value counts capped daily withdrawals until the balance falls below its wage target. It may be inaccurate during a 30-day pause."
			+ "</body></html>";
	}

	private static JTextArea valueArea(String text, Color color, String family, int size)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setForeground(color);
		area.setFont(new Font(family, Font.PLAIN, size));
		area.setBorder(BorderFactory.createEmptyBorder());
		area.setMinimumSize(new Dimension(0, 18));
		return area;
	}

	private static final class LedgerTabbedPaneUI extends BasicTabbedPaneUI
	{
		@Override
		protected void installDefaults()
		{
			super.installDefaults();
			contentBorderInsets = new Insets(1, 1, 1, 1);
			tabInsets = new Insets(6, 11, 6, 11);
			selectedTabPadInsets = new Insets(0, 0, 0, 0);
		}

		@Override
		protected void paintTabBackground(Graphics graphics, int placement, int index,
			int x, int y, int width, int height, boolean selected)
		{
			graphics.setColor(selected ? BURGUNDY : CARD);
			graphics.fillRect(x, y, width, height);
		}

		@Override
		protected void paintTabBorder(Graphics graphics, int placement, int index,
			int x, int y, int width, int height, boolean selected)
		{
			graphics.setColor(selected ? GOLD : BORDER);
			graphics.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
		}

		@Override
		protected void paintText(Graphics graphics, int placement, Font font,
			java.awt.FontMetrics metrics, int index, String title,
			java.awt.Rectangle textRect, boolean selected)
		{
			graphics.setColor(selected ? GOLD : MUTED);
			super.paintText(graphics, placement, font, metrics, index, title, textRect, selected);
		}

		@Override
		protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex)
		{
			int y = calculateTabAreaHeight(placement, runCount, maxTabHeight);
			graphics.setColor(DEEP_GOLD);
			graphics.drawLine(0, y, tabPane.getWidth(), y);
		}
	}

	private static final class GothicBorder extends AbstractBorder
	{
		@Override
		public Insets getBorderInsets(Component component)
		{
			return new Insets(5, 5, 5, 5);
		}

		@Override
		public Insets getBorderInsets(Component component, Insets insets)
		{
			insets.set(5, 5, 5, 5);
			return insets;
		}

		@Override
		public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(BORDER);
			g.drawRect(x, y, width - 1, height - 1);
			g.setColor(DEEP_GOLD);
			int corner = 8;
			g.drawLine(x + 2, y + 2, x + corner, y + 2);
			g.drawLine(x + 2, y + 2, x + 2, y + corner);
			g.drawLine(x + width - corner - 1, y + 2, x + width - 3, y + 2);
			g.drawLine(x + width - 3, y + 2, x + width - 3, y + corner);
			g.drawLine(x + 2, y + height - 3, x + corner, y + height - 3);
			g.drawLine(x + 2, y + height - corner - 1, x + 2, y + height - 3);
			g.drawLine(x + width - corner - 1, y + height - 3, x + width - 3, y + height - 3);
			g.drawLine(x + width - 3, y + height - corner - 1, x + width - 3, y + height - 3);
			g.dispose();
		}
	}

	static final class Status
	{
		final int approvalPercent;
		final int coffer;
		final int wageTarget;
		final boolean royalTrouble;
		final long updatesUntilBelowTarget;
		final String untilUpdate;
		final boolean inKingdom;
		final String collectionLog;

		Status(int approvalPercent, int coffer, int wageTarget, boolean royalTrouble,
			long updatesUntilBelowTarget, String untilUpdate, boolean inKingdom,
			String collectionLog)
		{
			this.approvalPercent = approvalPercent;
			this.coffer = coffer;
			this.wageTarget = wageTarget;
			this.royalTrouble = royalTrouble;
			this.updatesUntilBelowTarget = updatesUntilBelowTarget;
			this.untilUpdate = untilUpdate;
			this.inKingdom = inKingdom;
			this.collectionLog = collectionLog;
		}
	}
}
