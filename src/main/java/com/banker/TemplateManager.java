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

import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;

@Singleton
class TemplateManager
{
	private static final List<String> TEMPLATE_NAMES = ImmutableList.of("Default", "Frequent Use", "Skilling");

	List<String> getTemplateNames()
	{
		return TEMPLATE_NAMES;
	}

	List<Category> createTemplate(String name)
	{
		switch (name)
		{
			case "Default":
				return createDefaultTemplate();
			case "Frequent Use":
				return createFrequentUseTemplate();
			case "Skilling":
				return createSkillingTemplate();
			default:
				return new ArrayList<>();
		}
	}

	private List<Category> createDefaultTemplate()
	{
		List<Category> template = new ArrayList<>();
		template.add(new Category("Melee Gear", new Color(178, 34, 34), ItemID.ABYSSAL_WHIP)
			.collects(ItemKind.MELEE_GEAR));
		template.add(new Category("Ranged Gear", new Color(46, 139, 46), ItemID.TOXIC_BLOWPIPE)
			.collects(ItemKind.RANGED_GEAR));
		template.add(new Category("Magic Gear", new Color(65, 105, 225), ItemID.TOXIC_TOTS_CHARGED)
			.collects(ItemKind.MAGIC_GEAR));
		template.add(new Category("Potions", new Color(148, 0, 211), ItemID._4DOSEPRAYERRESTORE)
			.collects(ItemKind.POTIONS));
		template.add(new Category("Food", new Color(210, 105, 30), ItemID.SHARK)
			.collects(ItemKind.FOOD));
		template.add(new Category("Runes", new Color(70, 130, 180), ItemID.LAWRUNE)
			.collects(ItemKind.RUNES));
		template.add(new Category("Herblore", new Color(34, 139, 34), ItemID.UNIDENTIFIED_RANARR)
			.collects(ItemKind.HERBLORE));
		template.add(new Category("Farming", new Color(154, 205, 50), ItemID.RANARR_SEED)
			.collects(ItemKind.FARMING));
		template.add(new Category("Slayer", new Color(95, 158, 160), ItemID.SLAYER_HELM_I)
			.collects(ItemKind.SLAYER));
		template.add(new Category("Clues", new Color(184, 134, 11), ItemID.TRAIL_CLUE_MASTER)
			.collects(ItemKind.CLUES));
		template.add(new Category("Skilling", new Color(139, 69, 19), ItemID.DRAGON_PICKAXE)
			.collects(ItemKind.MINING_SMITHING, ItemKind.WOODCUTTING_FLETCHING, ItemKind.FISHING_COOKING,
				ItemKind.CRAFTING, ItemKind.RUNECRAFTING, ItemKind.HUNTER, ItemKind.CONSTRUCTION,
				ItemKind.TOOLS, ItemKind.PRAYER));
		template.add(new Category("Loot", new Color(255, 215, 0), ItemID.COINS)
			.collects(ItemKind.LOOT, ItemKind.COINS));
		template.add(new Category("Misc", new Color(112, 128, 144), ItemID.CHRONICLE)
			.collects(ItemKind.TELEPORTS, ItemKind.GEAR, ItemKind.COSMETICS, ItemKind.PETS,
				ItemKind.EVERYTHING_ELSE));
		return template;
	}

	private List<Category> createFrequentUseTemplate()
	{
		List<Category> template = new ArrayList<>();
		template.add(new Category("Everyday", new Color(112, 128, 144), ItemID.CHRONICLE)
			.collects(ItemKind.TELEPORTS, ItemKind.GEAR, ItemKind.CLUES, ItemKind.LOOT, ItemKind.COINS,
				ItemKind.PRAYER, ItemKind.SLAYER, ItemKind.EVERYTHING_ELSE));
		template.add(new Category("Melee", new Color(178, 34, 34), ItemID.ABYSSAL_WHIP)
			.collects(ItemKind.MELEE_GEAR));
		template.add(new Category("Ranged", new Color(46, 139, 46), ItemID.TOXIC_BLOWPIPE)
			.collects(ItemKind.RANGED_GEAR));
		template.add(new Category("Magic", new Color(65, 105, 225), ItemID.TOXIC_TOTS_CHARGED)
			.collects(ItemKind.MAGIC_GEAR, ItemKind.RUNES));
		template.add(new Category("Food", new Color(210, 105, 30), ItemID.SHARK)
			.collects(ItemKind.FOOD));
		template.add(new Category("Consumables", new Color(148, 0, 211), ItemID._4DOSEPRAYERRESTORE)
			.collects(ItemKind.POTIONS));
		template.add(new Category("Resources", new Color(139, 69, 19), ItemID.DRAGON_PICKAXE)
			.collects(ItemKind.MINING_SMITHING, ItemKind.WOODCUTTING_FLETCHING, ItemKind.FISHING_COOKING,
				ItemKind.CRAFTING, ItemKind.RUNECRAFTING, ItemKind.HUNTER, ItemKind.CONSTRUCTION,
				ItemKind.TOOLS));
		template.add(new Category("Herbs/Seeds", new Color(34, 139, 34), ItemID.UNIDENTIFIED_RANARR)
			.collects(ItemKind.HERBLORE, ItemKind.FARMING));
		template.add(new Category("Fashionscape", new Color(255, 105, 180), ItemID.RED_PARTYHAT)
			.collects(ItemKind.COSMETICS, ItemKind.PETS)
			.filters("*(t)", "*(g)"));
		return template;
	}

	private List<Category> createSkillingTemplate()
	{
		List<Category> template = new ArrayList<>();
		template.add(new Category("Misc/Quests", new Color(112, 128, 144), ItemID.SKILLCAPE_QP)
			.collects(ItemKind.TELEPORTS, ItemKind.GEAR, ItemKind.CLUES, ItemKind.SLAYER,
				ItemKind.PRAYER, ItemKind.HUNTER, ItemKind.CONSTRUCTION, ItemKind.TOOLS,
				ItemKind.EVERYTHING_ELSE));
		template.add(new Category("Melee", new Color(178, 34, 34), ItemID.ABYSSAL_WHIP)
			.collects(ItemKind.MELEE_GEAR));
		template.add(new Category("Ranged", new Color(46, 139, 46), ItemID.TOXIC_BLOWPIPE)
			.collects(ItemKind.RANGED_GEAR));
		template.add(new Category("Magic", new Color(65, 105, 225), ItemID.TOXIC_TOTS_CHARGED)
			.collects(ItemKind.MAGIC_GEAR, ItemKind.RUNES, ItemKind.RUNECRAFTING));
		template.add(new Category("Food/Fishing", new Color(210, 105, 30), ItemID.SHARK)
			.collects(ItemKind.FOOD, ItemKind.FISHING_COOKING));
		template.add(new Category("Potions/Herbs", new Color(148, 0, 211), ItemID._4DOSEPRAYERRESTORE)
			.collects(ItemKind.POTIONS, ItemKind.HERBLORE, ItemKind.FARMING));
		template.add(new Category("Woodcutting/Fletching", new Color(139, 69, 19), ItemID.DRAGON_AXE)
			.collects(ItemKind.WOODCUTTING_FLETCHING));
		template.add(new Category("Mining/Smithing", new Color(105, 105, 105), ItemID.DRAGON_PICKAXE)
			.collects(ItemKind.MINING_SMITHING, ItemKind.CRAFTING));
		template.add(new Category("Loot", new Color(255, 215, 0), ItemID.COINS)
			.collects(ItemKind.LOOT, ItemKind.COINS));
		return template;
	}
}
