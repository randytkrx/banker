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

import lombok.Getter;

/**
 * Broad item types the auto-categorizer can recognize. Categories declare
 * which kinds they collect; the alias list is the fallback used to match
 * kinds onto categories by name when nothing claims them explicitly.
 */
enum ItemKind
{
	MELEE_GEAR("Melee gear", "melee"),
	RANGED_GEAR("Ranged gear & ammo", "ranged", "range"),
	MAGIC_GEAR("Magic gear", "magic", "mage"),
	GEAR("Other equipment", "equipment", "armour", "armor"),
	POTIONS("Potions", "potion", "consumable", "brew"),
	FOOD("Food", "food"),
	RUNES("Runes", "rune"),
	TELEPORTS("Teleports", "teleport", "everyday", "misc", "other"),
	PRAYER("Bones & prayer", "prayer", "bones"),
	HERBLORE("Herblore supplies", "herblore", "herb"),
	FARMING("Seeds & farming", "farming", "farm", "seed"),
	SLAYER("Slayer", "slayer"),
	MINING_SMITHING("Mining & smithing", "mining", "smithing", "ore", "skilling"),
	WOODCUTTING_FLETCHING("Woodcutting & fletching", "woodcutting", "fletching", "logs", "skilling"),
	FISHING_COOKING("Fishing & cooking", "fishing", "cooking", "skilling"),
	CRAFTING("Crafting", "crafting", "skilling"),
	RUNECRAFTING("Runecrafting", "runecraft", "skilling"),
	HUNTER("Hunter", "hunter", "skilling"),
	CONSTRUCTION("Construction", "construction", "skilling"),
	TOOLS("Tools & supplies", "tool", "supplies", "resource", "skilling"),
	CLUES("Clues & caskets", "clue", "treasure"),
	COSMETICS("Cosmetics & fun", "cosmetic", "costume", "fashion", "fun", "holiday"),
	PETS("Pets", "pets", "pet"),
	COINS("Coins & currency", "coins", "coin", "currency", "money"),
	LOOT("Loot & valuables", "loot", "valuable", "treasury"),
	EVERYTHING_ELSE("Everything else", "everything", "misc", "other");

	@Getter
	private final String displayName;

	@Getter
	private final String[] aliases;

	ItemKind(String displayName, String... aliases)
	{
		this.displayName = displayName;
		this.aliases = aliases;
	}
}
