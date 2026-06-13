# Banker

Banker lets you sort your bank into color coded categories. Every item in your bank can belong to a category, and the plugin can place items into the right category for you automatically. Categories show up as colors on your bank tabs, as highlights on the items themselves, and as a list in a side panel where everything is managed.

## Categories

A category is a named group of items with a color, an icon, and rules describing what belongs in it. The plugin starts you off with a ready made set of categories covering gear, food, potions, runes, skilling materials and more. You can rename them, recolor them, delete them, or build your own from scratch. Two special categories always exist. The Main Tab category is pinned to the top of the list and represents the first tab of your bank, so you can decide what belongs in your main tab like any other category. The New Items category collects things you bank for the first time that the plugin could not confidently place, so you can sort them later.

## Automatic sorting

Each category can claim broad item types using simple checkboxes. The types cover melee gear, ranged gear and ammo, magic gear, other equipment, potions, food, runes, teleports, bones and prayer items, herblore supplies, seeds and farming, slayer items, the gathering and production skills, clues, cosmetics and fun items, pets, coins and currency, loot, and a catch all called everything else. A category with the everything else type receives anything that nothing else claims, which means your whole bank can be covered with no item left behind.

For finer control every category also accepts name filters. A filter is a piece of text that matches against item names, with the star character acting as a wildcard. For example a filter of raw star sends all raw food to that category, and star ore sends every ore. Name filters are the strongest rule and always win over the type checkboxes.

When a new item enters your bank the plugin classifies it by reading its name and equipment stats and places it in the matching category right away. The Auto assign button in the panel does the same for your whole bank at once. It offers two modes. New items only places unsorted items and re evaluates things the plugin placed earlier, while leaving anything you assigned by hand exactly where you put it. Re sort everything rebuilds all placements from your current rules. Editing a category's rules re sorts automatically, so the bank keeps itself organized the way you configured it.

You can also assign items by hand. Right click any item in the bank and a Banker submenu lets you add it to or remove it from any category. Items assigned by hand are pinned and the automatic sorting will never move them.

## Seeing your categories in the bank

The plugin colors the tab row at the top of your bank. The first tab takes the color of the Main Tab category and the numbered tabs take the colors of your categories in panel order, so dragging categories up and down in the panel decides which tab gets which color. The tab you are currently viewing gets a white outline.

Assigned items are highlighted in their category color. You can choose between a colored underline, an outline, or a filled tint in the settings. Hovering an item shows its category name as a tooltip.

Clicking a category in the side panel filters the bank to show only that category's items, and the bank title changes to show what you are viewing. Clicking again, or clicking any normal bank tab, shows everything again. The panel also has an Unassigned row that shows how many bank items are in no category at all, and clicking it filters the bank to just those items so you can clean them up.

There is also an optional helper for physically rearranging your bank. When enabled, items whose actual bank tab does not match their category's tab get a small red corner marker, so you can drag them to the right place and watch the markers disappear.

## The side panel

The panel lists every category with its icon, color, item count, and the exchange value of its items currently in your bank. Drag rows to reorder, click a row to view it in the bank, and use the edit and delete buttons on each row. The editor lets you set the name, color, tab icon, type checkboxes and name filters, shows every assigned item as a grid where clicking an item removes it, and lets you add items through the in game item search.

Templates give you a fresh starting point with sensible categories and rules already set up. Your setup can be exported to a file or copied to the clipboard as a compact setup code, and either form can be imported, which makes it easy to back up your configuration or share it with friends.

## Working with other plugins

Banker draws no widgets inside the bank interface, so it works alongside Bank Tags and its tag tabs without conflict. All coloring is drawn as overlays on top of the bank.

## Settings

The settings let you toggle the top tab coloring, the item highlights and their style and opacity, the hover tooltip, the right click assignment menu, the misplaced item markers, the tracking of newly banked items, and whether new items are sorted automatically or collected for manual review.
