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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Automatic item-to-category matching. Resolution order per item:
 * <ol>
 * <li>a category whose name filters match the item name (in category order)</li>
 * <li>a category that claims the item's {@link ItemKind} via its kind checkboxes</li>
 * <li>a category whose name matches the kind's aliases (e.g. "Potions/Herbs")</li>
 * <li>a category claiming {@link ItemKind#EVERYTHING_ELSE}, so every item can land somewhere</li>
 * </ol>
 * Must be used on the client thread (it reads item compositions).
 */
@Singleton
class AutoCategorizer
{
	private static final Pattern DOSE_SUFFIX = Pattern.compile(".*\\([1-9]\\)$");

	private static final List<String> HERBLORE_HINTS = ImmutableList.of(
		"grimy ", "herb", " vial", "(unf)", "limpwurt", "snape grass", "spiders' eggs", "unicorn horn",
		"mort myre fungus", "white berries", "wine of zamorak", "eye of newt", "crushed nest", "potato cactus",
		"blue dragon scale", "amylase", "jangerberries", "cactus spine", "lily of the sands", "nail beast nails",
		"poison ivy berries", "crushed superior dragon bones", "volcanic ash", "swamp toad", "toad's legs",
		"chocolate dust", "zulrah's scales", "dragon scale dust", "goat horn dust", "desert goat horn",
		"silver dust", "ashes mix", "caviar");
	// clean herbs have no shared keyword, so match them by name prefix
	// (prefix instead of contains so e.g. "spirit shield" doesn't match "irit")
	private static final List<String> CLEAN_HERB_PREFIXES = ImmutableList.of(
		"guam", "marrentill", "tarromin", "harralander", "ranarr weed", "toadflax", "irit leaf", "avantoe",
		"kwuarm", "huasca", "snapdragon", "cadantine", "lantadyme", "dwarf weed", "torstol");
	private static final List<String> FARMING_HINTS = ImmutableList.of(
		" seed", " sapling", " seedling", "compost", " spore", " bush", " acorn", "plant cure", "secateurs",
		"watering can", "gricoller's can", "gardening trowel", "bottomless bucket", "seed box", "seed dibber");
	private static final List<String> COSMETIC_HINTS = ImmutableList.of(
		"partyhat", "h'ween mask", "santa ", "santa hat", "easter", "halloween", "rubber chicken",
		"ornament kit", "costume", "bunny ", "scythe (", "yo-yo", "marionette", "jack lantern",
		"grim reaper hood", "banshee ", "mime ", "zombie head", "afro", "flared trousers", "sleeping cap",
		"fancy boots", "fighting boots", "paper hat", "birthday ", "cracker", "tinsel", "antlers",
		"bobble ", "jester ", "tri-jester", "woolly ", "gnome child", "event rpg", "stale baguette",
		"toy ", "clockwork", "fishbowl helmet", "diving apparatus");
	private static final Set<String> PET_NAMES = ImmutableSet.of(
		"pet chaos elemental", "pet dagannoth supreme", "pet dagannoth prime", "pet dagannoth rex",
		"pet penance queen", "pet kree'arra", "pet general graardor", "pet zilyana", "pet k'ril tsutsaroth",
		"baby mole", "prince black dragon", "kalphite princess", "pet smoke devil", "pet kraken",
		"pet dark core", "pet snakeling", "chompy chick", "venenatis spiderling", "callisto cub",
		"vet'ion jr.", "scorpia's offspring", "tzrek-jad", "hellpuppy", "abyssal orphan", "heron",
		"rock golem", "beaver", "baby chinchompa", "bloodhound", "giant squirrel", "tangleroot",
		"rift guardian", "rocky", "phoenix", "olmlet", "skotos", "jal-nib-rek", "herbi", "noon",
		"vorki", "lil' zik", "ikkle hydra", "sraracha", "youngllef", "smolcano", "little nightmare",
		"lil' creator", "tiny tempor", "nexling", "abyssal protector", "tumeken's guardian", "muphin",
		"wisp", "butch", "lil'viathan", "baron", "scurry", "smol heredit", "quetzin", "nid", "huberte",
		"moxi", "bran", "yami", "dom", "soup", "gull (pet)", "beef");
	private static final List<String> CURRENCY_HINTS = ImmutableList.of(
		"tokkul", " token", "ticket", "mark of grace", "numulite", "blood money", "trading sticks",
		"golden nugget", "stardust", "unidentified minerals", "ecto-token", "abyssal pearls",
		"hallowed mark", "molch pearl", "ancient shard", "frog token", "warrior guild token");
	private static final List<String> PRAYER_HINTS = ImmutableList.of(
		" bones", "ensouled", " ashes", "demonic ashes", "fiendish ashes", "bonemeal");
	private static final List<String> SLAYER_HINTS = ImmutableList.of(
		"slayer", "black mask", "facemask", "earmuffs", "nose peg", "spiny helmet", "leaf-bladed", "broad ",
		"witchwood icon", "rock hammer", "bag of salt", "ice cooler", "fungicide");
	private static final List<String> RUNECRAFTING_HINTS = ImmutableList.of(
		"essence", "talisman", " tiara", "small pouch", "medium pouch", "large pouch", "giant pouch",
		"colossal pouch", "abyssal lantern");
	private static final List<String> HUNTER_HINTS = ImmutableList.of(
		"impling", "box trap", "bird snare", "butterfly", "noose wand", "deadfall", "rabbit snare",
		"hunter ", "camouflage", "birdhouse");
	private static final List<String> CONSTRUCTION_HINTS = ImmutableList.of(
		" plank", "limestone brick", " nails", "marble block", "gold leaf");
	private static final List<String> MINING_HINTS = ImmutableList.of(
		"pickaxe", " ore", " bar", "coal", "dynamite", "sandstone", "granite", "limestone", "amethyst",
		"coal bag", "blast furnace");
	private static final List<String> WOODCUTTING_HINTS = ImmutableList.of(
		"logs", " axe", "arrow shaft", "bowstring", "bow string", "flax", "bow (u)",
		"log basket", "feather", " tip", "arrowtips", "javelin heads", "headless arrow");
	private static final List<String> FISHING_HINTS = ImmutableList.of(
		"fishing", "harpoon", "raw ", "burnt ", " bait", "fish barrel", "lobster pot", "karambwan vessel",
		"jug of water", "grapes", "sandworms");
	private static final List<String> CRAFTING_HINTS = ImmutableList.of(
		"uncut ", "sapphire", "emerald", "ruby", "diamond", "dragonstone", "opal", " jade", "red topaz",
		"zenyte", "molten glass", "glassblowing", " hide", "leather", "needle", "thread", " mould",
		"bolt of cloth", "wool", "soft clay", "clay", "seaweed", "battlestaff");
	private static final List<String> TOOLS_HINTS = ImmutableList.of(
		"rope", "tinderbox", "chisel", " hammer", " saw", "knife", "bucket", "pestle", "spade",
		"gem bag", "herb sack", "swamp tar", "small fishing net", "looting bag");
	private static final List<String> RANGED_AMMO_HINTS = ImmutableList.of(
		" arrow", " bolts", " bolt", " dart", " javelin", "throwing axe", "thrownaxe", "chinchompa");
	// fletching materials that would otherwise match the ammo hints
	private static final List<String> AMMO_EXCLUSIONS = ImmutableList.of("shaft", " tip", "(unf", "unfinished");
	private static final List<String> JEWELLERY_TELEPORT_HINTS = ImmutableList.of(
		"ring of dueling", "games necklace", "amulet of glory", "burning amulet", "skills necklace",
		"ring of wealth", "combat bracelet", "digsite pendant", "necklace of passage", "ring of returning",
		"xeric's talisman", "drakan's medallion", "royal seed pod", "ectophial", "enchanted lyre",
		"skull sceptre", "pharaoh's sceptre", "camulet", "ring of the elements", "slayer ring");
	private static final Set<String> FOOD_NAMES = ImmutableSet.of(
		"shrimps", "sardine", "herring", "mackerel", "trout", "cod", "pike", "salmon", "tuna", "lobster",
		"bass", "swordfish", "monkfish", "shark", "sea turtle", "manta ray", "dark crab", "anglerfish",
		"karambwan", "cooked karambwan", "peach", "banana", "cabbage", "potato", "bread", "cheese",
		"pineapple", "watermelon", "strawberry", "papaya fruit", "orange", "tomato", "onion", "egg",
		"cooked meat", "cooked chicken", "cooked karambwanji", "chocolate bar", "purple sweets",
		"jug of wine");
	private static final List<String> FOOD_HINTS = ImmutableList.of(
		" pie", " pizza", " cake", " stew", " curry", "potato with", " kebab", "wine of", " batta",
		"chocolate ", "roast ", " crunchies", " gnomebowl", "tea ");

	private final ItemManager itemManager;

	@Inject
	private AutoCategorizer(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	/**
	 * Match the given (canonical) item ids onto the given categories.
	 * Items that resolve to no category are omitted from the result.
	 */
	Map<Category, List<Integer>> categorize(Collection<Integer> itemIds, List<Category> categories)
	{
		List<CompiledFilter> filters = compileFilters(categories);
		Map<ItemKind, Category> kindMapping = mapKindsToCategories(categories);
		Category catchAll = kindMapping.get(ItemKind.EVERYTHING_ELSE);
		Map<Category, List<Integer>> result = new LinkedHashMap<>();

		for (int itemId : itemIds)
		{
			ItemComposition composition = itemManager.getItemComposition(itemId);
			String name = composition.getName().toLowerCase();

			Category category = matchFilters(filters, name);
			if (category == null)
			{
				Category mapped = kindMapping.get(classify(itemId, name));
				category = mapped != null ? mapped : catchAll;
			}

			if (category != null)
			{
				result.computeIfAbsent(category, c -> new ArrayList<>()).add(itemId);
			}
		}

		return result;
	}

	// --- category resolution ---

	private static class CompiledFilter
	{
		final Category category;
		final Pattern glob; // null for plain substring filters
		final String substring;

		CompiledFilter(Category category, String filter)
		{
			this.category = category;
			if (filter.indexOf('*') >= 0)
			{
				StringBuilder regex = new StringBuilder();
				for (String part : filter.split("\\*", -1))
				{
					if (regex.length() > 0)
					{
						regex.append(".*");
					}
					regex.append(Pattern.quote(part));
				}
				this.glob = Pattern.compile(regex.toString());
				this.substring = null;
			}
			else
			{
				this.glob = null;
				this.substring = filter;
			}
		}

		boolean matches(String name)
		{
			return glob != null ? glob.matcher(name).matches() : name.contains(substring);
		}
	}

	private static List<CompiledFilter> compileFilters(List<Category> categories)
	{
		List<CompiledFilter> compiled = new ArrayList<>();
		for (Category category : categories)
		{
			if (category.isNewItems())
			{
				continue;
			}

			for (String filter : category.getNameFilters())
			{
				compiled.add(new CompiledFilter(category, filter));
			}
		}
		return compiled;
	}

	private static Category matchFilters(List<CompiledFilter> filters, String name)
	{
		for (CompiledFilter filter : filters)
		{
			if (filter.matches(name))
			{
				return filter.category;
			}
		}
		return null;
	}

	private static Map<ItemKind, Category> mapKindsToCategories(List<Category> categories)
	{
		Map<ItemKind, Category> mapping = new EnumMap<>(ItemKind.class);

		for (ItemKind kind : ItemKind.values())
		{
			// explicit claims (kind checkboxes) win, in category order
			for (Category category : categories)
			{
				if (!category.isNewItems() && category.getKinds().contains(kind))
				{
					mapping.put(kind, category);
					break;
				}
			}

			if (mapping.containsKey(kind))
			{
				continue;
			}

			// otherwise fall back to matching the kind's aliases against category names
			outer:
			for (String alias : kind.getAliases())
			{
				for (Category category : categories)
				{
					if (!category.isNewItems() && category.getName().toLowerCase().contains(alias))
					{
						mapping.put(kind, category);
						break outer;
					}
				}
			}
		}

		return mapping;
	}

	// --- item classification ---

	private ItemKind classify(int itemId, String name)
	{
		if (name.contains("clue scroll") || name.contains("reward casket") || name.contains("challenge scroll")
			|| name.contains("puzzle box") || name.contains("clue bottle") || name.contains("scroll box"))
		{
			return ItemKind.CLUES;
		}

		if (name.endsWith(" rune") || name.equals("rune pouch"))
		{
			return ItemKind.RUNES;
		}

		// cosmetics and pets early: some have combat stats or food-like names
		if (containsAny(name, COSMETIC_HINTS))
		{
			return ItemKind.COSMETICS;
		}

		if (PET_NAMES.contains(name))
		{
			return ItemKind.PETS;
		}

		// before the potion dose check: slayer items and teleport jewellery
		// also carry "(n)" charge suffixes
		if (containsAny(name, SLAYER_HINTS))
		{
			return ItemKind.SLAYER;
		}

		if (name.contains("teleport") || name.endsWith(" tablet") || containsAny(name, JEWELLERY_TELEPORT_HINTS))
		{
			return ItemKind.TELEPORTS;
		}

		if (containsAny(name, RANGED_AMMO_HINTS) && !containsAny(name, AMMO_EXCLUSIONS))
		{
			return ItemKind.RANGED_GEAR;
		}

		// watering cans carry "(1)"-"(8)" fill suffixes like potion doses do
		if (DOSE_SUFFIX.matcher(name).matches() && !name.contains("watering can"))
		{
			return ItemKind.POTIONS;
		}

		// plain "bones" has no leading space, so the hint list misses it
		if (containsAny(name, PRAYER_HINTS) || name.equals("bones"))
		{
			return ItemKind.PRAYER;
		}

		// farming before herblore so "toadflax seed" beats the "toadflax" herb prefix
		// ("rake" is matched exactly so drake items don't end up in farming)
		if (containsAny(name, FARMING_HINTS) || name.equals("rake"))
		{
			return ItemKind.FARMING;
		}

		// "roe" is matched exactly because it appears inside many other words
		if (containsAny(name, HERBLORE_HINTS) || startsWithAny(name, CLEAN_HERB_PREFIXES)
			|| name.equals("roe") || (name.endsWith(" weed") && !name.contains("seed")))
		{
			return ItemKind.HERBLORE;
		}

		// skill material checks; order matters where hints overlap
		// (e.g. "limestone brick" before "limestone", "pickaxe" before " axe")
		if (containsAny(name, RUNECRAFTING_HINTS))
		{
			return ItemKind.RUNECRAFTING;
		}
		if (containsAny(name, HUNTER_HINTS))
		{
			return ItemKind.HUNTER;
		}
		if (containsAny(name, CONSTRUCTION_HINTS))
		{
			return ItemKind.CONSTRUCTION;
		}
		// chocolate bar would otherwise match " bar"
		if (containsAny(name, MINING_HINTS) && !name.contains("chocolate"))
		{
			return ItemKind.MINING_SMITHING;
		}
		if (containsAny(name, WOODCUTTING_HINTS))
		{
			return ItemKind.WOODCUTTING_FLETCHING;
		}
		if (containsAny(name, FISHING_HINTS))
		{
			return ItemKind.FISHING_COOKING;
		}
		if (containsAny(name, CRAFTING_HINTS))
		{
			return ItemKind.CRAFTING;
		}
		if (containsAny(name, TOOLS_HINTS))
		{
			return ItemKind.TOOLS;
		}

		if (FOOD_NAMES.contains(name) || containsAny(name, FOOD_HINTS))
		{
			return ItemKind.FOOD;
		}

		if (name.equals("coins") || containsAny(name, CURRENCY_HINTS))
		{
			return ItemKind.COINS;
		}

		if (name.endsWith(" key") || name.contains("casket") || name.contains("crystal shard"))
		{
			return ItemKind.LOOT;
		}

		// grand exchange item sets aren't equipable, so catch them by name
		if (name.contains("armour set") || name.contains(" set (lg)") || name.contains(" set (sm)"))
		{
			return ItemKind.GEAR;
		}

		ItemStats stats = itemManager.getItemStats(itemId);
		if (stats != null && stats.isEquipable() && stats.getEquipment() != null)
		{
			ItemEquipmentStats eq = stats.getEquipment();

			// compare attack bonus magnitudes rather than checking styles in a
			// fixed order: many melee weapons carry a small +magic bonus
			// (e.g. every dagger has +1 magic attack)
			int meleeAtk = Math.max(Math.max(eq.getAstab(), eq.getAslash()), eq.getAcrush());
			int rangedAtk = eq.getArange();
			int magicAtk = eq.getAmagic();
			if (meleeAtk > 0 || rangedAtk > 0 || magicAtk > 0)
			{
				if (meleeAtk >= rangedAtk && meleeAtk >= magicAtk)
				{
					return ItemKind.MELEE_GEAR;
				}
				return rangedAtk >= magicAtk ? ItemKind.RANGED_GEAR : ItemKind.MAGIC_GEAR;
			}

			// no attack bonuses; try damage and strength bonuses
			if (eq.getStr() > 0)
			{
				return ItemKind.MELEE_GEAR;
			}
			if (eq.getRstr() > 0)
			{
				return ItemKind.RANGED_GEAR;
			}
			if (eq.getMdmg() > 0)
			{
				return ItemKind.MAGIC_GEAR;
			}

			// defensive-only gear: melee armour either penalizes magic defence
			// or defends best against melee styles (e.g. anti-dragon shield)
			int meleeDef = Math.max(Math.max(eq.getDstab(), eq.getDslash()), eq.getDcrush());
			if (eq.getDmagic() < 0 || meleeDef > Math.max(eq.getDmagic(), eq.getDrange()))
			{
				return ItemKind.MELEE_GEAR;
			}
			if (eq.getDmagic() > meleeDef)
			{
				return ItemKind.MAGIC_GEAR;
			}
			return ItemKind.GEAR;
		}

		return ItemKind.EVERYTHING_ELSE;
	}

	private static boolean containsAny(String name, List<String> hints)
	{
		for (String hint : hints)
		{
			if (name.contains(hint))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean startsWithAny(String name, List<String> prefixes)
	{
		for (String prefix : prefixes)
		{
			if (name.startsWith(prefix))
			{
				return true;
			}
		}
		return false;
	}
}
