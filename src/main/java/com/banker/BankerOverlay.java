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
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

@Singleton
class BankerOverlay extends WidgetItemOverlay
{
	private static final int[] TAB_COUNT_VARBITS = {
		VarbitID.BANK_TAB_1, VarbitID.BANK_TAB_2, VarbitID.BANK_TAB_3,
		VarbitID.BANK_TAB_4, VarbitID.BANK_TAB_5, VarbitID.BANK_TAB_6,
		VarbitID.BANK_TAB_7, VarbitID.BANK_TAB_8, VarbitID.BANK_TAB_9,
	};
	private static final Color MISPLACED_COLOR = new Color(255, 60, 60, 230);
	private static final int MISPLACED_MARKER_SIZE = 9;

	private final Client client;
	private final ItemManager itemManager;
	private final CategoryManager categoryManager;
	private final BankerConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	private BankerOverlay(Client client, ItemManager itemManager, CategoryManager categoryManager,
		BankerConfig config, TooltipManager tooltipManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.categoryManager = categoryManager;
		this.config = config;
		this.tooltipManager = tooltipManager;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Category category = categoryManager.getCategoryForItem(itemManager.canonicalize(itemId));
		if (category == null)
		{
			return;
		}

		final Color color = category.getAwtColor();
		final Rectangle bounds = widgetItem.getCanvasBounds();

		if (config.showOverlay())
		{
			switch (config.highlightStyle())
			{
				case FILL:
					int opacity = Math.max(0, Math.min(255, config.fillOpacity()));
					graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
					graphics.fill(bounds);
					graphics.setColor(color);
					graphics.draw(bounds);
					break;
				case OUTLINE:
					graphics.setColor(color);
					graphics.draw(bounds);
					break;
				case UNDERLINE:
					graphics.setColor(color);
					graphics.fillRect(bounds.x + 1, bounds.y + bounds.height - 2, bounds.width - 1, 3);
					break;
			}
		}

		if (config.highlightMisplaced() && isMisplaced(category, widgetItem))
		{
			graphics.setColor(MISPLACED_COLOR);
			Polygon corner = new Polygon(
				new int[]{bounds.x, bounds.x + MISPLACED_MARKER_SIZE, bounds.x},
				new int[]{bounds.y, bounds.y, bounds.y + MISPLACED_MARKER_SIZE},
				3);
			graphics.fill(corner);
		}

		if (config.showCategoryTooltip())
		{
			Point mouse = client.getMouseCanvasPosition();
			if (mouse != null && bounds.contains(mouse.getX(), mouse.getY()))
			{
				tooltipManager.add(new Tooltip(ColorUtil.wrapWithColorTag(category.getName(), color)));
			}
		}
	}

	/**
	 * An item is misplaced when the physical bank tab its slot belongs to
	 * differs from the tab its category is mapped onto (panel order:
	 * Main Tab = tab 0, then tabs 1-9).
	 */
	private boolean isMisplaced(Category category, WidgetItem widgetItem)
	{
		int targetTab = targetTabFor(category);
		if (targetTab < 0)
		{
			return false; // category has no mapped physical tab
		}

		if (widgetItem.getWidget() == null)
		{
			return false;
		}

		return physicalTabOfSlot(widgetItem.getWidget().getIndex()) != targetTab;
	}

	private int targetTabFor(Category category)
	{
		if (category.isNewItems())
		{
			return -1;
		}
		if (category.isMainTab())
		{
			return 0;
		}

		int index = 1;
		for (Category c : categoryManager.getCategories())
		{
			if (c.isNewItems() || c.isMainTab())
			{
				continue;
			}
			if (c == category)
			{
				return index <= TAB_COUNT_VARBITS.length ? index : -1;
			}
			index++;
		}
		return -1;
	}

	private int physicalTabOfSlot(int slot)
	{
		int start = 0;
		for (int tab = 0; tab < TAB_COUNT_VARBITS.length; tab++)
		{
			int count = client.getVarbitValue(TAB_COUNT_VARBITS[tab]);
			if (slot < start + count)
			{
				return tab + 1;
			}
			start += count;
		}
		return 0; // remaining slots belong to the main tab
	}
}
