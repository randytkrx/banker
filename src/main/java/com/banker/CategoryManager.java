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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Owns the category list and its persistence. Holds no UI references;
 * interested parties subscribe via {@link #setChangeListener(Runnable)}.
 * Collections are concurrent so categories can be read from the client
 * thread (overlay, bank filter) while being mutated from the EDT.
 */
@Slf4j
@Singleton
class CategoryManager
{
	static final String NEW_ITEMS_CATEGORY_NAME = "New Items";
	static final int MAX_NAME_LENGTH = 40;
	static final String SHARE_CODE_PREFIX = "banker:";

	private static final String CATEGORIES_KEY = "categories";
	private static final Type CATEGORY_LIST_TYPE = new TypeToken<List<Category>>() {}.getType();
	private static final Color NEW_ITEMS_COLOR = new Color(0, 200, 255);

	private final ConfigManager configManager;
	private final Gson gson;
	private final TemplateManager templateManager;

	@Getter
	private final List<Category> categories = new CopyOnWriteArrayList<>();

	private volatile Map<Integer, Category> itemLookup = Collections.emptyMap();

	/**
	 * Invoked after every mutation, on whichever thread made the change.
	 */
	@Setter
	private Runnable changeListener;

	@Inject
	CategoryManager(ConfigManager configManager, Gson gson, TemplateManager templateManager)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.templateManager = templateManager;
	}

	void load()
	{
		categories.clear();

		String json = configManager.getConfiguration(BankerConfig.GROUP, CATEGORIES_KEY);
		if (!Strings.isNullOrEmpty(json))
		{
			try
			{
				List<Category> loaded = gson.fromJson(json, CATEGORY_LIST_TYPE);
				if (loaded != null)
				{
					loaded.stream()
						.filter(c -> c != null && !Strings.isNullOrEmpty(c.getName()))
						.forEach(c -> categories.add(sanitize(c)));
				}
			}
			catch (Exception e)
			{
				log.warn("Unable to parse saved categories, starting fresh", e);
			}
		}

		if (categories.isEmpty())
		{
			templateManager.createTemplate("Default").forEach(c -> categories.add(sanitize(c)));
		}

		ensureMainTabCategory();
		ensureNewItemsCategory();
		rebuildLookup();
	}

	Category addCategory(String name, Color color, int iconItemId, Set<ItemKind> kinds, List<String> nameFilters)
	{
		Category category = new Category(name, color, iconItemId);
		category.setKinds(copyKinds(kinds));
		category.setNameFilters(copyFilters(nameFilters));
		sanitize(category);
		categories.add(category);
		changed();
		return category;
	}

	/**
	 * Move a category to the given position. Order determines both the
	 * bank tab order and which category wins when several claim the same
	 * item kind.
	 */
	void moveCategory(Category category, int targetIndex)
	{
		if (category.isMainTab())
		{
			return; // pinned to the top
		}

		int current = categories.indexOf(category);
		if (current < 0)
		{
			return;
		}

		// nothing sorts above the pinned main tab
		int minIndex = !categories.isEmpty() && categories.get(0).isMainTab() ? 1 : 0;
		int clamped = Math.max(minIndex, Math.min(targetIndex, categories.size() - 1));
		if (clamped == current)
		{
			return;
		}

		// targetIndex is the desired final position; inserting into the list
		// shortened by the removal lands the category exactly there
		categories.remove(category);
		categories.add(clamped, category);
		changed();
	}

	void removeCategory(Category category)
	{
		if (category.isNewItems() || category.isMainTab())
		{
			return;
		}

		categories.remove(category);
		changed();
	}

	void updateCategory(Category category, String newName, Color newColor, int iconItemId,
		Set<ItemKind> kinds, List<String> nameFilters)
	{
		if (!category.isNewItems())
		{
			category.setName(clampName(newName));
		}
		category.setAwtColor(newColor);
		category.setIconItemId(iconItemId);
		category.setKinds(copyKinds(kinds));
		category.setNameFilters(copyFilters(nameFilters));
		changed();
	}

	void toggleItem(Category category, int itemId)
	{
		if (!category.getItemIds().remove(itemId))
		{
			addItemInternal(category, itemId);
		}
		changed();
	}

	void addItem(Category category, int itemId)
	{
		addItemInternal(category, itemId);
		changed();
	}

	void removeItem(Category category, int itemId)
	{
		category.getItemIds().remove(itemId);
		category.getAutoItemIds().remove(itemId);
		changed();
	}

	void clearItems(Category category)
	{
		category.getItemIds().clear();
		category.getAutoItemIds().clear();
		changed();
	}

	/**
	 * Apply auto-categorizer results. Items move out of whatever category
	 * previously held them automatically (or New Items), but manually pinned
	 * assignments are left alone. Returns the number of items that moved.
	 */
	int applyAutoAssignments(Map<Category, ? extends Collection<Integer>> assignments)
	{
		int moved = 0;
		for (Map.Entry<Category, ? extends Collection<Integer>> entry : assignments.entrySet())
		{
			Category target = entry.getKey();
			for (int itemId : entry.getValue())
			{
				Category current = itemLookup.get(itemId);
				if (current == target)
				{
					continue;
				}

				if (current != null)
				{
					if (!current.isNewItems() && !current.isAutoAssigned(itemId))
					{
						continue; // manually pinned; never moved automatically
					}
					current.getItemIds().remove(itemId);
					current.getAutoItemIds().remove(itemId);
				}

				target.getItemIds().add(itemId);
				target.getAutoItemIds().add(itemId);
				moved++;
			}
		}

		if (moved > 0)
		{
			changed();
		}
		return moved;
	}

	void addToNewItems(Collection<Integer> itemIds)
	{
		Category newItems = getNewItemsCategory();
		if (newItems == null || itemIds.isEmpty())
		{
			return;
		}

		newItems.getItemIds().addAll(itemIds);
		changed();
	}

	/**
	 * Replace all user categories with the given list. The existing
	 * New Items category (and the items collected in it) is preserved.
	 */
	void replaceCategories(List<Category> replacement)
	{
		Category existingNewItems = getNewItemsCategory();
		Category existingMainTab = getMainTabCategory();

		categories.clear();
		replacement.stream()
			.filter(c -> c != null && !Strings.isNullOrEmpty(c.getName()) && !c.isNewItems() && !c.isMainTab())
			.forEach(c -> categories.add(sanitize(c)));

		if (existingMainTab != null)
		{
			categories.add(0, existingMainTab);
		}
		if (existingNewItems != null)
		{
			categories.add(existingNewItems);
		}
		ensureMainTabCategory();
		ensureNewItemsCategory();
		changed();
	}

	String exportJson()
	{
		return gson.toJson(categories);
	}

	/**
	 * Compact clipboard-friendly setup code: gzipped json, base64 encoded.
	 */
	String exportShareCode()
	{
		try
		{
			java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
			try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bytes))
			{
				gzip.write(exportJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
			return SHARE_CODE_PREFIX + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
		}
		catch (java.io.IOException e)
		{
			log.warn("Failed to build share code", e);
			return null;
		}
	}

	boolean importShareCode(String code)
	{
		if (code == null)
		{
			return false;
		}

		String trimmed = code.trim();
		if (!trimmed.startsWith(SHARE_CODE_PREFIX))
		{
			return false;
		}

		try
		{
			byte[] compressed = java.util.Base64.getDecoder().decode(trimmed.substring(SHARE_CODE_PREFIX.length()));
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed)))
			{
				byte[] buffer = new byte[8192];
				int read;
				while ((read = gzip.read(buffer)) != -1)
				{
					out.write(buffer, 0, read);
				}
			}
			return importJson(new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
		}
		catch (Exception e)
		{
			log.warn("Failed to parse share code", e);
			return false;
		}
	}

	boolean importJson(String json)
	{
		final List<Category> imported;
		try
		{
			imported = gson.fromJson(json, CATEGORY_LIST_TYPE);
		}
		catch (Exception e)
		{
			log.warn("Failed to parse imported categories", e);
			return false;
		}

		if (imported == null || imported.stream().noneMatch(c -> c != null && !Strings.isNullOrEmpty(c.getName())))
		{
			return false;
		}

		replaceCategories(imported);
		return true;
	}

	Category getCategoryForItem(int itemId)
	{
		return itemLookup.get(itemId);
	}

	boolean isItemAssigned(int itemId)
	{
		return itemLookup.containsKey(itemId);
	}

	Category getNewItemsCategory()
	{
		return categories.stream()
			.filter(Category::isNewItems)
			.findFirst()
			.orElse(null);
	}

	boolean isNameAvailable(String name, Category ignore)
	{
		String trimmed = name.trim();
		return categories.stream()
			.filter(c -> c != ignore)
			.noneMatch(c -> c.getName().equalsIgnoreCase(trimmed));
	}

	private void addItemInternal(Category category, int itemId)
	{
		category.getItemIds().add(itemId);
		// an explicit assignment pins the item; auto-sorting won't move it again
		category.getAutoItemIds().remove(itemId);

		// assigning an item to a real category graduates it out of New Items
		if (!category.isNewItems())
		{
			Category newItems = getNewItemsCategory();
			if (newItems != null)
			{
				newItems.getItemIds().remove(itemId);
				newItems.getAutoItemIds().remove(itemId);
			}
		}
	}

	Category getMainTabCategory()
	{
		return categories.stream()
			.filter(Category::isMainTab)
			.findFirst()
			.orElse(null);
	}

	/**
	 * The pinned category representing the bank's main tab (tab 0). Always
	 * first so users can decide what their first tab collects.
	 */
	private void ensureMainTabCategory()
	{
		Category mainTab = getMainTabCategory();
		if (mainTab == null)
		{
			mainTab = new Category("Main Tab", new Color(200, 200, 200), net.runelite.api.gameval.ItemID.COINS);
			mainTab.setMainTab(true);
			categories.add(0, mainTab);
		}
		else if (categories.indexOf(mainTab) != 0)
		{
			categories.remove(mainTab);
			categories.add(0, mainTab);
		}
	}

	private void ensureNewItemsCategory()
	{
		if (getNewItemsCategory() != null)
		{
			return;
		}

		Category newItems = new Category(NEW_ITEMS_CATEGORY_NAME, NEW_ITEMS_COLOR, net.runelite.api.gameval.ItemID.BANK_FILLER);
		newItems.setNewItems(true);
		categories.add(newItems);
	}

	private Category sanitize(Category category)
	{
		category.setName(clampName(category.getName()));

		Set<Integer> concurrent = ConcurrentHashMap.newKeySet();
		if (category.getItemIds() != null)
		{
			concurrent.addAll(category.getItemIds());
		}
		category.setItemIds(concurrent);

		Set<Integer> auto = ConcurrentHashMap.newKeySet();
		if (category.getAutoItemIds() != null)
		{
			auto.addAll(category.getAutoItemIds());
			auto.retainAll(concurrent);
		}
		category.setAutoItemIds(auto);

		category.setKinds(copyKinds(category.getKinds()));
		category.setNameFilters(copyFilters(category.getNameFilters()));
		return category;
	}

	private static Set<ItemKind> copyKinds(Set<ItemKind> kinds)
	{
		Set<ItemKind> copy = ConcurrentHashMap.newKeySet();
		if (kinds != null)
		{
			// gson maps unrecognized enum names to null entries; drop them
			kinds.stream().filter(java.util.Objects::nonNull).forEach(copy::add);
		}
		return copy;
	}

	private static List<String> copyFilters(List<String> filters)
	{
		List<String> copy = new CopyOnWriteArrayList<>();
		if (filters != null)
		{
			filters.stream()
				.filter(f -> f != null && !f.trim().isEmpty())
				.map(f -> f.trim().toLowerCase())
				.forEach(copy::add);
		}
		return copy;
	}

	private String clampName(String name)
	{
		String trimmed = name.trim();
		return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
	}

	private void changed()
	{
		rebuildLookup();
		save();

		Runnable listener = changeListener;
		if (listener != null)
		{
			listener.run();
		}
	}

	private void rebuildLookup()
	{
		Map<Integer, Category> lookup = new HashMap<>();
		for (Category category : categories)
		{
			for (int itemId : category.getItemIds())
			{
				lookup.putIfAbsent(itemId, category);
			}
		}
		itemLookup = lookup;
	}

	private void save()
	{
		configManager.setConfiguration(BankerConfig.GROUP, CATEGORIES_KEY, gson.toJson(categories));
	}
}
