package com.miscellaniamonitor;

import com.google.inject.Provides;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Miscellania Monitor",
	description = "Read-only upkeep and reset monitor for Managing Miscellania",
	tags = {"kingdom", "approval", "coffer", "upkeep"},
	enabledByDefault = false
)
public class MiscellaniaMonitorPlugin extends Plugin
{
	private static final Gson GSON = new Gson();
	private static final Type COLLECTION_LOG_TYPE = new TypeToken<ArrayList<CollectionLogEntry>>() {}.getType();
	private static final DateTimeFormatter COLLECTION_LOG_DATE = DateTimeFormatter
		.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());
	private static final int MAX_APPROVAL = 127;
	private static final int BASE_WAGE = 50_000;
	private static final int ROYAL_TROUBLE_WAGE = 75_000;
	private static final Set<Integer> KINGDOM_REGIONS = new HashSet<>(Arrays.asList(10044, 10300));

	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private MiscellaniaMonitorConfig config;
	@Inject private ItemManager itemManager;

	private final MiscellaniaMonitorPanel panel = new MiscellaniaMonitorPanel(this::clearCollectionLog);
	private final List<CollectionLogEntry> collectionHistory = new ArrayList<>();
	private NavigationButton navigationButton;
	private String collectionHistoryPlayer;
	private boolean collectionLoggedForOpen;
	private String lastLoggedCollectionItems;
	private long lastLoggedCollectionAt;

	@Provides
	MiscellaniaMonitorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MiscellaniaMonitorConfig.class);
	}

	@Override
	protected void startUp()
	{
		navigationButton = NavigationButton.builder()
			.tooltip("Miscellania Monitor")
			.icon(createIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshPanel(isInKingdom());
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		refreshPanel(isInKingdom());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.MISC_APPROVAL || event.getVarbitId() == VarbitID.MISC_COFFERS)
		{
			refreshPanel(isInKingdom());
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.MISC_BOTH_MANAGE
			|| event.getGroupId() == InterfaceID.MISC_COLLECTION)
		{
			refreshPanel(isInKingdom());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			collectionLoggedForOpen = false;
			SwingUtilities.invokeLater(() -> panel.showUnavailable("Log in to read kingdom status"));
		}
	}

	private boolean isInKingdom()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		WorldPoint location = client.getLocalPlayer().getWorldLocation();
		return location != null && KINGDOM_REGIONS.contains(location.getRegionID());
	}

	private boolean isInterfaceVisible(int packedWidgetId)
	{
		Widget widget = client.getWidget(packedWidgetId);
		return widget != null && !widget.isHidden();
	}

	private String readVisibleCollectionItems()
	{
		Widget collection = client.getWidget(InterfaceID.MiscCollection.UNIVERSE);
		Widget items = client.getWidget(InterfaceID.MiscCollection.ITEMS);
		if (collection == null || collection.isHidden() || items == null || items.isHidden())
		{
			return "";
		}

		Map<Integer, Integer> itemQuantities = new TreeMap<>();
		collectVisibleItems(items, itemQuantities, new HashSet<>());
		if (itemQuantities.isEmpty())
		{
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (Map.Entry<Integer, Integer> item : itemQuantities.entrySet())
		{
			if (result.length() > 0)
			{
				result.append(", ");
			}
			ItemComposition composition = itemManager.getItemComposition(item.getKey());
			String name = composition == null || composition.getName() == null
				? "Item " + item.getKey()
				: composition.getName();
			result.append(name).append(" ").append(String.format("%,d", item.getValue()));
		}
		return result.toString();
	}

	private static void collectVisibleItems(Widget widget, Map<Integer, Integer> quantities, Set<Integer> visited)
	{
		if (widget == null || widget.isHidden() || !visited.add(widget.getId()))
		{
			return;
		}

		int itemId = widget.getItemId();
		int itemQuantity = widget.getItemQuantity();
		if (itemId > 0 && itemQuantity > 0)
		{
			quantities.merge(itemId, itemQuantity, Integer::sum);
		}

		collectVisibleItems(widget.getDynamicChildren(), quantities, visited);
		collectVisibleItems(widget.getStaticChildren(), quantities, visited);
		collectVisibleItems(widget.getNestedChildren(), quantities, visited);
	}

	private static void collectVisibleItems(Widget[] widgets, Map<Integer, Integer> quantities, Set<Integer> visited)
	{
		if (widgets == null)
		{
			return;
		}
		for (Widget child : widgets)
		{
			collectVisibleItems(child, quantities, visited);
		}
	}

	private void refreshPanel(boolean inKingdom)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			SwingUtilities.invokeLater(() -> panel.showUnavailable("Log in to read kingdom status"));
			return;
		}
		if (client.getVarpValue(VarPlayerID.MISC_QUEST) <= 0)
		{
			SwingUtilities.invokeLater(() -> panel.showUnavailable("Complete Throne of Miscellania to unlock status"));
			return;
		}

		int rawApproval = client.getVarbitValue(VarbitID.MISC_APPROVAL);
		int coffer = client.getVarbitValue(VarbitID.MISC_COFFERS);
		if (!inKingdom && (rawApproval <= 0 || coffer <= 0))
		{
			SwingUtilities.invokeLater(() -> panel.showUnavailable("Kingdom values unavailable · waiting for valid client data"));
			return;
		}

		boolean royalTrouble = Quest.ROYAL_TROUBLE.getState(client) == QuestState.FINISHED;
		int approvalPercent = Math.max(0, Math.min(100, rawApproval * 100 / MAX_APPROVAL));
		int dailyWageCap = royalTrouble ? ROYAL_TROUBLE_WAGE : BASE_WAGE;
		int wageTarget = dailyWageCap * 10;
		long updatesUntilBelowTarget = coffer < wageTarget ? 0 : (coffer - wageTarget) / dailyWageCap + 1L;
		boolean collectionOpen = isInterfaceVisible(InterfaceID.MiscCollection.UNIVERSE);
		String collectionItems = collectionOpen ? readVisibleCollectionItems() : "";
		ensureCollectionHistoryLoaded();
		if (!collectionOpen)
		{
			collectionLoggedForOpen = false;
		}
		else if (!collectionLoggedForOpen && !collectionItems.isEmpty())
		{
			collectionLoggedForOpen = true;
			long nowMillis = System.currentTimeMillis();
			boolean duplicateReopen = collectionItems.equals(lastLoggedCollectionItems)
				&& nowMillis - lastLoggedCollectionAt < 60_000;
			if (!duplicateReopen)
			{
				collectionHistory.add(new CollectionLogEntry(nowMillis, collectionItems));
				lastLoggedCollectionItems = collectionItems;
				lastLoggedCollectionAt = nowMillis;
				configManager.setRSProfileConfiguration("miscellaniamonitor", "collectionLog",
					GSON.toJson(collectionHistory));
			}
		}
		String collectionLog = formatCollectionLog();
		Instant now = Instant.now();
		String untilUpdate = formatDuration(Duration.between(now,
			now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)));

		MiscellaniaMonitorPanel.Status status = new MiscellaniaMonitorPanel.Status(
			approvalPercent, coffer, wageTarget, royalTrouble, updatesUntilBelowTarget, untilUpdate,
			inKingdom, collectionLog);
		SwingUtilities.invokeLater(() -> panel.update(status, config.approvalWarningThreshold()));
	}

	private void ensureCollectionHistoryLoaded()
	{
		String playerName = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
		if (playerName == null || playerName.equals(collectionHistoryPlayer))
		{
			return;
		}

		collectionHistoryPlayer = playerName;
		collectionHistory.clear();
		String savedLog = configManager.getRSProfileConfiguration("miscellaniamonitor", "collectionLog");
		if (savedLog != null && !savedLog.isEmpty())
		{
			try
			{
				List<CollectionLogEntry> savedEntries = GSON.fromJson(savedLog, COLLECTION_LOG_TYPE);
				if (savedEntries != null)
				{
					collectionHistory.addAll(savedEntries);
				}
			}
			catch (RuntimeException ignored)
			{
				collectionHistory.clear();
			}
		}

		if (!collectionHistory.isEmpty())
		{
			CollectionLogEntry lastEntry = collectionHistory.get(collectionHistory.size() - 1);
			lastLoggedCollectionItems = lastEntry.items;
			lastLoggedCollectionAt = lastEntry.timestampMillis;
		}
		collectionLoggedForOpen = false;
	}

	private String formatCollectionLog()
	{
		if (collectionHistory.isEmpty())
		{
			return "";
		}

		StringBuilder log = new StringBuilder();
		for (int i = collectionHistory.size() - 1; i >= 0; i--)
		{
			CollectionLogEntry entry = collectionHistory.get(i);
			if (log.length() > 0)
			{
				log.append('\n');
			}
			log.append(COLLECTION_LOG_DATE.format(Instant.ofEpochMilli(entry.timestampMillis)))
				.append(" — ").append(entry.items);
		}
		return log.toString();
	}

	private void clearCollectionLog()
	{
		clientThread.invokeLater(() -> {
			ensureCollectionHistoryLoaded();
			collectionHistory.clear();
			lastLoggedCollectionItems = null;
			lastLoggedCollectionAt = 0;
			configManager.setRSProfileConfiguration("miscellaniamonitor", "collectionLog", "[]");
			SwingUtilities.invokeLater(panel::clearCollectionLog);
		});
	}

	private static final class CollectionLogEntry
	{
		private long timestampMillis;
		private String items;

		private CollectionLogEntry(long timestampMillis, String items)
		{
			this.timestampMillis = timestampMillis;
			this.items = items;
		}
	}

	private static String formatDuration(Duration duration)
	{
		long seconds = Math.max(0, duration.getSeconds());
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		return String.format("%dh %02dm", hours, minutes);
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int[] crownX = {2, 5, 7, 9, 12, 13, 11, 4};
		int[] crownY = {4, 7, 2, 7, 4, 11, 11, 11};
		graphics.setColor(new Color(72, 54, 29));
		graphics.fillPolygon(crownX, crownY, crownX.length);
		graphics.setColor(new Color(198, 162, 95));
		graphics.fillPolygon(crownX, crownY, crownX.length);
		graphics.fillRect(4, 12, 8, 2);
		graphics.setColor(new Color(111, 82, 44));
		graphics.drawPolygon(crownX, crownY, crownX.length);
		graphics.drawLine(4, 14, 12, 14);
		graphics.dispose();
		return image;
	}
}
