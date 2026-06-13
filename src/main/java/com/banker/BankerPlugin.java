/*
 * Copyright (c) 2026, Randy <nightlight681@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.banker;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxItemSearch;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.util.ColorUtil;

@Slf4j
@PluginDescriptor(
	name = "Banker",
	description = "Organize your bank into color-coded categories with tabs, highlights and a sorting panel",
	tags = {"bank", "tags", "categories", "organize", "highlight", "sort"}
)
public class BankerPlugin extends Plugin
{
	private static final String KNOWN_ITEMS_KEY = "knownItems";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BankerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private net.runelite.client.ui.overlay.OverlayManager overlayManager;

	@Inject
	private BankerOverlay overlay;

	@Inject
	private BankTopTabsOverlay topTabsOverlay;

	@Inject
	private BankViewFilter viewFilter;

	@Inject
	private ItemManager itemManager;

	@Inject
	private CategoryManager categoryManager;

	@Inject
	private TemplateManager templateManager;

	@Inject
	private AutoCategorizer autoCategorizer;

	@Inject
	private ChatboxItemSearch itemSearch;

	@Inject
	private EventBus eventBus;

	@Getter
	@Inject
	private ColorPickerManager colorPickerManager;

	private BankerPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		categoryManager.load();
		categoryManager.setChangeListener(this::onCategoriesChanged);

		panel = new BankerPanel(this, categoryManager, templateManager, itemManager);
		navButton = NavigationButton.builder()
			.tooltip("Banker")
			.icon(createPanelIcon())
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(overlay);
		overlayManager.add(topTabsOverlay);

		viewFilter.setActiveViewListener(() -> SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.refreshList();
			}
		}));
		eventBus.register(viewFilter);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(viewFilter);
		categoryManager.setChangeListener(null);
		clientThread.invoke(viewFilter::shutdown);

		overlayManager.remove(overlay);
		overlayManager.remove(topTabsOverlay);
		clientToolbar.removeNavigation(navButton);

		panel = null;
		navButton = null;
	}

	@Provides
	BankerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankerConfig.class);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK || !config.trackNewItems())
		{
			return;
		}

		Set<Integer> current = new HashSet<>();
		for (Item item : event.getItemContainer().getItems())
		{
			int id = item.getId();
			if (id > 0 && id != ItemID.BANK_FILLER)
			{
				current.add(itemManager.canonicalize(id));
			}
		}

		if (current.isEmpty())
		{
			return;
		}

		String saved = configManager.getRSProfileConfiguration(BankerConfig.GROUP, KNOWN_ITEMS_KEY);
		if (Strings.isNullOrEmpty(saved))
		{
			// first look at this profile's bank: seed the baseline, nothing is "new"
			saveKnownItems(current);
			return;
		}

		Set<Integer> known = new HashSet<>();
		for (String s : saved.split(","))
		{
			try
			{
				known.add(Integer.parseInt(s));
			}
			catch (NumberFormatException ignored)
			{
			}
		}

		List<Integer> fresh = current.stream()
			.filter(id -> !known.contains(id))
			.collect(Collectors.toList());
		if (fresh.isEmpty())
		{
			return;
		}

		known.addAll(fresh);
		saveKnownItems(known);

		List<Integer> unassigned = fresh.stream()
			.filter(id -> !categoryManager.isItemAssigned(id))
			.collect(Collectors.toList());
		if (unassigned.isEmpty())
		{
			return;
		}

		if (config.autoSortNewItems())
		{
			// place items we can confidently classify straight into a matching category
			Map<Category, List<Integer>> sorted = autoCategorizer.categorize(unassigned, categoryManager.getCategories());
			categoryManager.applyAutoAssignments(sorted);

			Set<Integer> placed = sorted.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.toSet());
			unassigned = unassigned.stream()
				.filter(id -> !placed.contains(id))
				.collect(Collectors.toList());
		}

		categoryManager.addToNewItems(unassigned);
	}

	/**
	 * Sort bank items into categories per the current rules. Re-evaluates
	 * unassigned items, New Items, and previous auto-placements; manual
	 * assignments only move when resortAll is set.
	 */
	void autoAssignBankItems(boolean resortAll, boolean showResult)
	{
		clientThread.invokeLater(() ->
		{
			net.runelite.api.ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank == null)
			{
				if (showResult)
				{
					SwingUtilities.invokeLater(() ->
					{
						if (panel != null)
						{
							panel.showAutoAssignResult(-1, List.of());
						}
					});
				}
				return;
			}

			Set<Integer> candidates = new HashSet<>();
			for (Item item : bank.getItems())
			{
				int id = item.getId();
				if (id <= 0 || id == ItemID.BANK_FILLER)
				{
					continue;
				}

				int canonical = itemManager.canonicalize(id);
				Category current = categoryManager.getCategoryForItem(canonical);
				if (resortAll || current == null || current.isNewItems() || current.isAutoAssigned(canonical))
				{
					candidates.add(canonical);
				}
			}

			Map<Category, List<Integer>> sorted = autoCategorizer.categorize(candidates, categoryManager.getCategories());
			if (resortAll)
			{
				// a full re-sort unpins everything: clear manual pins by marking
				// candidates auto in their current category first
				for (int id : candidates)
				{
					Category current = categoryManager.getCategoryForItem(id);
					if (current != null && current.containsItem(id))
					{
						current.getAutoItemIds().add(id);
					}
				}
			}
			int assigned = categoryManager.applyAutoAssignments(sorted);

			List<Integer> unmatched = candidates.stream()
				.filter(id ->
				{
					Category current = categoryManager.getCategoryForItem(id);
					return current == null || current.isNewItems();
				})
				.collect(Collectors.toList());

			if (showResult)
			{
				SwingUtilities.invokeLater(() ->
				{
					if (panel != null)
					{
						panel.showAutoAssignResult(assigned, unmatched);
					}
				});
			}
		});
	}

	/**
	 * Called when a category's kinds or name filters change so previous
	 * auto-placements immediately follow the new rules.
	 */
	void categoryRulesChanged()
	{
		autoAssignBankItems(false, false);
	}

	/**
	 * Sweep the given items into a catch-all category: an existing category
	 * claiming "Everything else" (or named Misc/Other), or a new "Other"
	 * category if there is none. Safe to call from the EDT.
	 */
	void assignToCatchAll(List<Integer> itemIds)
	{
		Category catchAll = categoryManager.getCategories().stream()
			.filter(c -> !c.isNewItems())
			.filter(c -> c.getKinds().contains(ItemKind.EVERYTHING_ELSE)
				|| c.getName().toLowerCase().contains("misc")
				|| c.getName().toLowerCase().contains("other"))
			.findFirst()
			.orElse(null);

		if (catchAll == null)
		{
			catchAll = categoryManager.addCategory("Other", new Color(112, 128, 144), ItemID.CASKET,
				Set.of(ItemKind.EVERYTHING_ELSE), List.of());
		}

		categoryManager.applyAutoAssignments(Map.of(catchAll, itemIds));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showCategoryMenu()
			|| event.getActionParam1() != InterfaceID.Bankmain.ITEMS
			|| !event.getOption().equals("Examine"))
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null)
		{
			return;
		}

		Widget itemWidget = container.getChild(event.getActionParam0());
		if (itemWidget == null)
		{
			return;
		}

		final int itemId = itemManager.canonicalize(itemWidget.getItemId());
		if (itemId <= 0 || itemId == ItemID.BANK_FILLER)
		{
			return;
		}

		MenuEntry parent = client.getMenu().createMenuEntry(-1)
			.setOption("Banker")
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE);
		Menu subMenu = parent.createSubMenu();

		for (Category category : categoryManager.getCategories())
		{
			boolean assigned = category.containsItem(itemId);
			subMenu.createMenuEntry(-1)
				.setOption(assigned ? "Remove from" : "Add to")
				.setTarget(ColorUtil.wrapWithColorTag(category.getName(), category.getAwtColor()))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> categoryManager.toggleItem(category, itemId));
		}

		subMenu.createMenuEntry(-1)
			.setOption("Edit categories")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> SwingUtilities.invokeLater(this::openOrganizerPanel));
	}

	/**
	 * Funnel for all category data changes; keeps the side panel and the
	 * bank view in sync regardless of which thread made the change.
	 */
	private void onCategoriesChanged()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.refreshList();
			}
		});
		clientThread.invokeLater(viewFilter::onCategoriesChanged);
	}

	// --- helpers for the Swing panel; all safe to call from the EDT ---

	void viewCategory(Category category)
	{
		clientThread.invokeLater(() -> viewFilter.toggleView(category));
	}

	boolean isViewing(Category category)
	{
		return viewFilter.getActiveCategory() == category;
	}

	void viewUnassigned()
	{
		clientThread.invokeLater(viewFilter::toggleUnassignedView);
	}

	boolean isViewingUnassigned()
	{
		return viewFilter.isUnassignedView();
	}

	/**
	 * Compute, per category, the GE value of its items currently in the bank,
	 * plus the count of bank items in no category at all. Results arrive on
	 * the EDT; both are null/-1 when the bank hasn't been seen yet.
	 */
	void computeBankStats(java.util.function.BiConsumer<Map<Category, Long>, Integer> consumer)
	{
		clientThread.invokeLater(() ->
		{
			net.runelite.api.ItemContainer bank = client.getItemContainer(InventoryID.BANK);
			if (bank == null)
			{
				SwingUtilities.invokeLater(() -> consumer.accept(null, -1));
				return;
			}

			Map<Category, Long> values = new LinkedHashMap<>();
			int unassigned = 0;
			for (Item item : bank.getItems())
			{
				int id = item.getId();
				if (id <= 0 || id == ItemID.BANK_FILLER)
				{
					continue;
				}

				int canonical = itemManager.canonicalize(id);
				Category category = categoryManager.getCategoryForItem(canonical);
				if (category == null)
				{
					unassigned++;
					continue;
				}

				long price = canonical == ItemID.COINS ? 1 : itemManager.getItemPrice(canonical);
				values.merge(category, price * item.getQuantity(), Long::sum);
			}

			final int unassignedCount = unassigned;
			SwingUtilities.invokeLater(() -> consumer.accept(values, unassignedCount));
		});
	}

	void openOrganizerPanel()
	{
		if (navButton != null)
		{
			clientToolbar.openPanel(navButton);
		}
	}

	void openEditor(Category category)
	{
		openOrganizerPanel();
		if (panel != null)
		{
			panel.openEditor(category);
		}
	}

	/**
	 * Open the in-game chatbox item search and hand the chosen (canonicalized)
	 * item id back on the EDT. Requires being logged in; otherwise the
	 * onUnavailable callback runs on the EDT instead.
	 */
	void searchForItem(String prompt, Consumer<Integer> onSelected, Runnable onUnavailable)
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				SwingUtilities.invokeLater(onUnavailable);
				return;
			}

			itemSearch
				.tooltipText(prompt)
				.onItemSelected(itemId ->
				{
					int canonical = itemManager.canonicalize(itemId);
					SwingUtilities.invokeLater(() -> onSelected.accept(canonical));
				})
				.build();
		});
	}

	/**
	 * Resolve item names on the client thread and deliver them to the EDT.
	 */
	void lookupItemNames(Collection<Integer> itemIds, Consumer<Map<Integer, String>> consumer)
	{
		List<Integer> ids = new ArrayList<>(itemIds);
		clientThread.invokeLater(() ->
		{
			Map<Integer, String> names = new LinkedHashMap<>();
			for (int id : ids)
			{
				names.put(id, itemManager.getItemComposition(id).getName());
			}
			SwingUtilities.invokeLater(() -> consumer.accept(names));
		});
	}

	private static BufferedImage createPanelIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setColor(new Color(178, 36, 36));
		g.fillRoundRect(0, 0, 7, 7, 3, 3);
		g.setColor(new Color(58, 148, 58));
		g.fillRoundRect(9, 0, 7, 7, 3, 3);
		g.setColor(new Color(61, 99, 185));
		g.fillRoundRect(0, 9, 7, 7, 3, 3);
		g.setColor(new Color(212, 160, 23));
		g.fillRoundRect(9, 9, 7, 7, 3, 3);
		g.dispose();
		return icon;
	}

	private void saveKnownItems(Set<Integer> known)
	{
		String csv = known.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(","));
		configManager.setRSProfileConfiguration(BankerConfig.GROUP, KNOWN_ITEMS_KEY, csv);
	}
}
