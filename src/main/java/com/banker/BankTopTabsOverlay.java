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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Tints the bank's own top tab row with category colors, mapped by panel
 * order: the 1st category colors tab 1, the 2nd colors tab 2, and so on
 * (the New Items bucket is skipped). The vanilla tab buttons are dynamic
 * children of the tabs container; they are identified by their "View tab"
 * menu actions and ordered by screen position.
 */
@Singleton
class BankTopTabsOverlay extends Overlay
{
	private static final int TINT_ALPHA = 90;
	private static final int UNDERLINE_HEIGHT = 4;

	private final Client client;
	private final BankerConfig config;
	private final CategoryManager categoryManager;

	@Inject
	private BankTopTabsOverlay(Client client, BankerConfig config, CategoryManager categoryManager)
	{
		this.client = client;
		this.config = config;
		this.categoryManager = categoryManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.colorBankTabs())
		{
			return null;
		}

		Widget tabsContainer = client.getWidget(InterfaceID.Bankmain.TABS);
		if (tabsContainer == null || tabsContainer.isHidden())
		{
			return null;
		}

		TabButtons buttons = findTabButtons(tabsContainer);
		int currentTab = client.getVarbitValue(VarbitID.BANK_CURRENTTAB);

		// the main/"view all" button is colored by the pinned Main Tab category;
		// it's matched explicitly so a missing button can't shift the numbered tabs
		Category mainTab = categoryManager.getMainTabCategory();
		if (buttons.allItems != null && mainTab != null)
		{
			paintTab(graphics, buttons.allItems, mainTab, currentTab == 0);
		}

		List<Category> numberedCategories = new ArrayList<>();
		for (Category category : categoryManager.getCategories())
		{
			if (!category.isNewItems() && !category.isMainTab())
			{
				numberedCategories.add(category);
			}
		}

		for (int i = 0; i < buttons.numbered.size() && i < numberedCategories.size(); i++)
		{
			paintTab(graphics, buttons.numbered.get(i), numberedCategories.get(i), currentTab == i + 1);
		}

		return null;
	}

	private static void paintTab(Graphics2D graphics, Widget button, Category category, boolean current)
	{
		Rectangle b = button.getBounds();
		if (b == null || b.width <= 0 || b.height <= 0)
		{
			return;
		}

		Color color = category.getAwtColor();

		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), TINT_ALPHA));
		graphics.fillRect(b.x + 2, b.y + 2, b.width - 4, b.height - 4);

		graphics.setColor(color);
		graphics.fillRect(b.x + 3, b.y + b.height - UNDERLINE_HEIGHT - 2, b.width - 6, UNDERLINE_HEIGHT);

		if (current)
		{
			graphics.setColor(Color.WHITE);
			graphics.drawRect(b.x + 1, b.y + 1, b.width - 3, b.height - 3);
		}
	}

	private static class TabButtons
	{
		Widget allItems;
		final List<Widget> numbered = new ArrayList<>();
	}

	/**
	 * The vanilla tab buttons: the "View all items" button and the numbered
	 * tabs, left to right.
	 */
	private static TabButtons findTabButtons(Widget tabsContainer)
	{
		TabButtons buttons = new TabButtons();
		collectTabButtons(tabsContainer.getChildren(), buttons);
		collectTabButtons(tabsContainer.getStaticChildren(), buttons);

		// the tab buttons might sit one layer deeper depending on how the
		// interface nests them
		Widget[] staticChildren = tabsContainer.getStaticChildren();
		if (staticChildren != null)
		{
			for (Widget child : staticChildren)
			{
				if (child != null)
				{
					collectTabButtons(child.getChildren(), buttons);
				}
			}
		}

		buttons.numbered.sort(Comparator.comparingInt(w -> w.getBounds() != null ? w.getBounds().x : 0));
		return buttons;
	}

	private static void collectTabButtons(Widget[] children, TabButtons buttons)
	{
		if (children == null)
		{
			return;
		}

		for (Widget child : children)
		{
			if (child == null || child.isHidden())
			{
				continue;
			}

			String[] actions = child.getActions();
			if (actions == null)
			{
				continue;
			}

			boolean numberedTab = false;
			boolean allItemsTab = false;
			for (String action : actions)
			{
				if (action == null)
				{
					continue;
				}
				if (action.equals("View all items"))
				{
					allItemsTab = true;
					numberedTab = false;
					break;
				}
				if (action.startsWith("View tab"))
				{
					numberedTab = true;
				}
			}

			if (allItemsTab && buttons.allItems == null)
			{
				buttons.allItems = child;
			}
			else if (numberedTab)
			{
				buttons.numbered.add(child);
			}
		}
	}
}
