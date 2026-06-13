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

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// identity equality is intentional: categories are mutable and compared by reference
@Getter
@Setter
@NoArgsConstructor
class Category
{
	private String name;
	// stored as packed rgb so the config json stays stable across jdk/gson versions
	private int color = 0xFFFFFF;
	private int iconItemId = -1;
	private Set<Integer> itemIds = ConcurrentHashMap.newKeySet();
	// the subset of itemIds placed by the auto-categorizer; these move freely
	// when rules change, unlike manual assignments which stay pinned
	private Set<Integer> autoItemIds = ConcurrentHashMap.newKeySet();
	private boolean newItems;
	// pinned first category representing the bank's main tab (tab 0)
	private boolean mainTab;
	// auto-assignment rules: broad item types this category collects,
	// plus name filters ("*rune", "raw *", plain substrings)
	private Set<ItemKind> kinds = ConcurrentHashMap.newKeySet();
	private List<String> nameFilters = new CopyOnWriteArrayList<>();

	Category(String name, Color color, int iconItemId)
	{
		this.name = name;
		this.iconItemId = iconItemId;
		setAwtColor(color);
	}

	Category collects(ItemKind... claimed)
	{
		kinds.addAll(Arrays.asList(claimed));
		return this;
	}

	Category filters(String... patterns)
	{
		nameFilters.addAll(Arrays.asList(patterns));
		return this;
	}

	Color getAwtColor()
	{
		return new Color(color & 0xFFFFFF);
	}

	void setAwtColor(Color c)
	{
		color = c.getRGB() & 0xFFFFFF;
	}

	boolean containsItem(int itemId)
	{
		return itemIds.contains(itemId);
	}

	boolean isAutoAssigned(int itemId)
	{
		return autoItemIds.contains(itemId);
	}
}
