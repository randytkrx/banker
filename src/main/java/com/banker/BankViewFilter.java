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

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.util.ColorUtil;

/**
 * Filters the bank down to the active category via the bankSearchFilter
 * script callback. Activation comes from the side panel; this creates no
 * bank widgets at all, so it coexists with Bank Tags' tag tabs.
 * Mutating methods must be called on the client thread.
 */
@Singleton
class BankViewFilter
{
	private final Client client;
	private final ItemManager itemManager;
	private final CategoryManager categoryManager;
	private final BankSearch bankSearch;

	/**
	 * Invoked (on the client thread) whenever the active category changes.
	 */
	@Setter
	private Runnable activeViewListener;

	@Getter
	private volatile Category activeCategory;

	/**
	 * When set, the bank is filtered to items not in any category.
	 */
	@Getter
	private volatile boolean unassignedView;

	@Inject
	private BankViewFilter(Client client, ItemManager itemManager, CategoryManager categoryManager,
		BankSearch bankSearch)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.categoryManager = categoryManager;
		this.bankSearch = bankSearch;
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!"bankSearchFilter".equals(event.getEventName()))
		{
			return;
		}

		Category active = activeCategory;
		boolean unassigned = unassignedView;
		if ((active == null && !unassigned) || client.getItemContainer(InventoryID.BANK) == null)
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		int itemId = intStack[intStackSize - 1];
		if (itemId < 0)
		{
			return;
		}

		int canonical = itemManager.canonicalize(itemId);
		boolean visible = unassigned
			? !categoryManager.isItemAssigned(canonical)
			: active.containsItem(canonical);
		if (!visible)
		{
			intStack[intStackSize - 2] = 0; // hide items outside the active view
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// the build script resets the title, so re-apply ours after every build
		if (event.getScriptId() == ScriptID.BANKMAIN_BUILD)
		{
			updateBankTitle();
		}
	}

	private void updateBankTitle()
	{
		Category active = activeCategory;
		if (active == null && !unassignedView)
		{
			return;
		}

		Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
		if (title == null)
		{
			return;
		}

		title.setText("Showing: " + (active != null
			? ColorUtil.wrapWithColorTag(active.getName(), active.getAwtColor())
			: "Unassigned items"));
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		// switching to a vanilla bank tab leaves category view, like banktags does
		if ((activeCategory != null || unassignedView)
			&& (event.getMenuOption().startsWith("View tab") || event.getMenuOption().equals("View all items")))
		{
			activeCategory = null;
			unassignedView = false;
			notifyActiveViewChanged();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN && event.isUnload()
			&& (activeCategory != null || unassignedView))
		{
			activeCategory = null;
			unassignedView = false;
			notifyActiveViewChanged();
		}
	}

	void toggleView(Category category)
	{
		unassignedView = false;
		if (activeCategory == category)
		{
			activeCategory = null;
		}
		else
		{
			activeCategory = category;
			client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
		}

		applyViewChange();
	}

	void toggleUnassignedView()
	{
		activeCategory = null;
		unassignedView = !unassignedView;
		if (unassignedView)
		{
			client.setVarbit(VarbitID.BANK_CURRENTTAB, 0);
		}

		applyViewChange();
	}

	private void applyViewChange()
	{
		if (isBankOpen())
		{
			client.playSoundEffect(SoundEffectID.UI_BOOP);
			bankSearch.reset(true);
		}

		notifyActiveViewChanged();
	}

	void onCategoriesChanged()
	{
		Category active = activeCategory;
		if (active == null && !unassignedView)
		{
			return;
		}

		if (active != null && !categoryManager.getCategories().contains(active))
		{
			activeCategory = null;
			if (isBankOpen())
			{
				bankSearch.layoutBank();
			}
			notifyActiveViewChanged();
		}
		else if (isBankOpen())
		{
			// assignments changed; refresh the filtered view
			bankSearch.layoutBank();
		}
	}

	void shutdown()
	{
		if (activeCategory != null || unassignedView)
		{
			activeCategory = null;
			unassignedView = false;
			if (isBankOpen())
			{
				bankSearch.layoutBank();
			}
			notifyActiveViewChanged();
		}
	}

	private boolean isBankOpen()
	{
		return client.getItemContainer(InventoryID.BANK) != null
			&& client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER) != null;
	}

	private void notifyActiveViewChanged()
	{
		if (activeViewListener != null)
		{
			activeViewListener.run();
		}
	}
}
