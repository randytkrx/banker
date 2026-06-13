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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(BankerConfig.GROUP)
public interface BankerConfig extends Config
{
	String GROUP = "banker";

	@ConfigItem(
		keyName = "colorBankTabs",
		name = "Color the bank's top tabs",
		description = "Tint your bank's own tabs (top row) with your category colors, matched by panel order:<br>" +
			"1st category = tab 1, 2nd = tab 2, ... Drag categories in the side panel to change the mapping.",
		position = 2
	)
	default boolean colorBankTabs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Highlight items",
		description = "Color-code assigned items in the bank with their category color",
		position = 3
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightStyle",
		name = "Highlight style",
		description = "How assigned items are highlighted in the bank",
		position = 3
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.UNDERLINE;
	}

	@Range(max = 255)
	@ConfigItem(
		keyName = "fillOpacity",
		name = "Fill opacity",
		description = "Transparency of the item highlight fill (0-255). Only used by the 'Fill + outline' style.",
		position = 4
	)
	default int fillOpacity()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "showCategoryMenu",
		name = "Right-click assignment",
		description = "Add a 'Banker' submenu when right-clicking bank items to quickly assign categories",
		position = 5
	)
	default boolean showCategoryMenu()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackNewItems",
		name = "Track new items",
		description = "Automatically collect items banked for the first time into a 'New Items' category so you can sort them later",
		position = 6
	)
	default boolean trackNewItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCategoryTooltip",
		name = "Category tooltip",
		description = "Show the item's category when hovering bank items",
		position = 8
	)
	default boolean showCategoryTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightMisplaced",
		name = "Mark misplaced items",
		description = "Mark items (red corner) whose physical bank tab doesn't match their category's tab<br>" +
			"(panel order: Main Tab = tab 0, then tabs 1-9). Useful while manually re-organizing your bank.",
		position = 9
	)
	default boolean highlightMisplaced()
	{
		return false;
	}

	@ConfigItem(
		keyName = "autoSortNewItems",
		name = "Auto-sort new items",
		description = "Try to automatically place newly banked items into a matching category (by item type)<br>" +
			"instead of 'New Items'. Requires 'Track new items'.",
		position = 7
	)
	default boolean autoSortNewItems()
	{
		return true;
	}
}
