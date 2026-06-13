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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

class CategoryEditorPanel extends JPanel
{
	private static final Color DEFAULT_COLOR = new Color(80, 160, 80);

	private final BankerPlugin plugin;
	private final CategoryManager categoryManager;
	private final BankerPanel mainPanel;
	private final ItemManager itemManager;
	private final Category category; // null when creating a new category

	private final JTextField nameField = new JTextField();
	private final JButton colorButton = new JButton();
	private final JLabel iconPreview = new JLabel();
	private final JPanel itemsGrid = new JPanel(new GridLayout(0, 5, 4, 4));
	private final JLabel itemsHeader = new JLabel();
	private final Map<ItemKind, JCheckBox> kindBoxes = new EnumMap<>(ItemKind.class);
	private final JTextField filtersField = new JTextField();

	private Color selectedColor;
	private int selectedIconId;

	CategoryEditorPanel(BankerPlugin plugin, CategoryManager categoryManager,
		BankerPanel mainPanel, ItemManager itemManager, Category category)
	{
		this.plugin = plugin;
		this.categoryManager = categoryManager;
		this.mainPanel = mainPanel;
		this.itemManager = itemManager;
		this.category = category;
		this.selectedColor = category != null ? category.getAwtColor() : DEFAULT_COLOR;
		this.selectedIconId = category != null ? category.getIconItemId() : ItemID.COINS;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildForm(), BorderLayout.NORTH);
		if (category != null)
		{
			add(buildItemsSection(), BorderLayout.CENTER);
			rebuildItemsGrid();
		}
		add(buildButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildForm()
	{
		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel(category == null ? "New category" : "Edit category");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(LEFT_ALIGNMENT);
		form.add(title);
		form.add(Box.createVerticalStrut(10));

		JLabel nameLabel = new JLabel("Name");
		nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);
		form.add(nameLabel);
		form.add(Box.createVerticalStrut(2));

		nameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameField.setForeground(Color.WHITE);
		nameField.setCaretColor(Color.WHITE);
		nameField.setBorder(new EmptyBorder(6, 6, 6, 6));
		nameField.setAlignmentX(LEFT_ALIGNMENT);
		nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		if (category != null)
		{
			nameField.setText(category.getName());
			if (category.isNewItems())
			{
				nameField.setEnabled(false);
				nameField.setToolTipText("The New Items category can't be renamed");
			}
		}
		form.add(nameField);
		form.add(Box.createVerticalStrut(8));

		// color + icon side by side
		JPanel pickRow = new JPanel(new GridLayout(1, 2, 8, 0));
		pickRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		pickRow.setAlignmentX(LEFT_ALIGNMENT);
		pickRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

		JPanel colorCol = new JPanel(new BorderLayout(0, 2));
		colorCol.setOpaque(false);
		JLabel colorLabel = new JLabel("Color");
		colorLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		colorLabel.setFont(FontManager.getRunescapeSmallFont());
		colorCol.add(colorLabel, BorderLayout.NORTH);

		colorButton.setFocusPainted(false);
		colorButton.setBackground(selectedColor);
		colorButton.setToolTipText("Pick the highlight color");
		colorButton.addActionListener(e -> openColorPicker());
		colorCol.add(colorButton, BorderLayout.CENTER);
		pickRow.add(colorCol);

		JPanel iconCol = new JPanel(new BorderLayout(4, 2));
		iconCol.setOpaque(false);
		JLabel iconLabel = new JLabel("Tab icon");
		iconLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		iconLabel.setFont(FontManager.getRunescapeSmallFont());
		iconCol.add(iconLabel, BorderLayout.NORTH);

		iconPreview.setPreferredSize(new Dimension(36, 32));
		iconPreview.setHorizontalAlignment(JLabel.CENTER);
		updateIconPreview();
		iconCol.add(iconPreview, BorderLayout.WEST);

		JButton iconButton = new JButton("PickÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦");
		iconButton.setFocusPainted(false);
		iconButton.setToolTipText("Search for an item to use as the tab icon (requires being logged in)");
		iconButton.addActionListener(e -> plugin.searchForItem("Use as icon",
			itemId ->
			{
				selectedIconId = itemId;
				updateIconPreview();
			},
			this::showLoginRequired));
		iconCol.add(iconButton, BorderLayout.CENTER);
		pickRow.add(iconCol);

		form.add(pickRow);

		// auto-assignment rules (not applicable to the New Items bucket)
		if (category == null || !category.isNewItems())
		{
			form.add(Box.createVerticalStrut(10));

			JLabel kindsLabel = new JLabel("Auto-assign these item types here:");
			kindsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			kindsLabel.setFont(FontManager.getRunescapeSmallFont());
			kindsLabel.setAlignmentX(LEFT_ALIGNMENT);
			form.add(kindsLabel);
			form.add(Box.createVerticalStrut(2));

			JPanel kindGrid = new JPanel(new GridLayout(0, 2, 2, 0));
			kindGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
			kindGrid.setAlignmentX(LEFT_ALIGNMENT);
			Set<ItemKind> selected = category != null ? category.getKinds() : new HashSet<>();
			for (ItemKind kind : ItemKind.values())
			{
				JCheckBox box = new JCheckBox(kind.getDisplayName());
				box.setFont(FontManager.getRunescapeSmallFont());
				box.setForeground(Color.WHITE);
				box.setBackground(ColorScheme.DARK_GRAY_COLOR);
				box.setFocusPainted(false);
				box.setSelected(selected.contains(kind));
				if (kind == ItemKind.EVERYTHING_ELSE)
				{
					box.setToolTipText("Catch-all: any item no other category claims ends up here");
				}
				kindBoxes.put(kind, box);
				kindGrid.add(box);
			}
			form.add(kindGrid);
			form.add(Box.createVerticalStrut(8));

			JLabel filtersLabel = new JLabel("Name filters (comma separated):");
			filtersLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			filtersLabel.setFont(FontManager.getRunescapeSmallFont());
			filtersLabel.setAlignmentX(LEFT_ALIGNMENT);
			form.add(filtersLabel);
			form.add(Box.createVerticalStrut(2));

			filtersField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			filtersField.setForeground(Color.WHITE);
			filtersField.setCaretColor(Color.WHITE);
			filtersField.setBorder(new EmptyBorder(6, 6, 6, 6));
			filtersField.setAlignmentX(LEFT_ALIGNMENT);
			filtersField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
			filtersField.setToolTipText("<html>Item name patterns that always land here, strongest rule.<br>" +
				"Plain text matches anywhere in the name; use * as a wildcard.<br>" +
				"Examples: <b>raw *</b>, <b>* ore</b>, <b>*logs*</b>, <b>teleport</b></html>");
			if (category != null)
			{
				filtersField.setText(String.join(", ", category.getNameFilters()));
			}
			form.add(filtersField);
		}

		return form;
	}

	private JPanel buildItemsSection()
	{
		JPanel section = new JPanel(new BorderLayout(0, 4));
		section.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		itemsHeader.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		itemsHeader.setFont(FontManager.getRunescapeSmallFont());
		header.add(itemsHeader, BorderLayout.WEST);

		JButton addItem = new JButton("Add itemÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦");
		addItem.setFocusPainted(false);
		addItem.setToolTipText("Search for an item to assign to this category (requires being logged in)");
		addItem.addActionListener(e -> plugin.searchForItem("Add to " + category.getName(),
			itemId ->
			{
				categoryManager.addItem(category, itemId);
				rebuildItemsGrid();
			},
			this::showLoginRequired));
		header.add(addItem, BorderLayout.EAST);
		section.add(header, BorderLayout.NORTH);

		itemsGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel gridWrapper = new JPanel(new BorderLayout());
		gridWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		gridWrapper.add(itemsGrid, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(gridWrapper,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARKER_GRAY_COLOR));
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		section.add(scrollPane, BorderLayout.CENTER);

		return section;
	}

	private JPanel buildButtons()
	{
		JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton cancel = new JButton("Cancel");
		cancel.setFocusPainted(false);
		cancel.addActionListener(e -> mainPanel.showListView());
		buttons.add(cancel);

		JButton save = new JButton("Save");
		save.setFocusPainted(false);
		save.addActionListener(e -> save());
		buttons.add(save);

		return buttons;
	}

	private void rebuildItemsGrid()
	{
		itemsGrid.removeAll();

		List<Integer> itemIds = new ArrayList<>(category.getItemIds());
		itemsHeader.setText("Assigned items (" + itemIds.size() + ")");

		for (int itemId : itemIds)
		{
			JLabel cell = new JLabel();
			cell.setPreferredSize(new Dimension(36, 32));
			cell.setHorizontalAlignment(JLabel.CENTER);
			cell.setToolTipText("Click to remove");
			itemManager.getImage(itemId).addTo(cell);
			cell.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					categoryManager.removeItem(category, itemId);
					rebuildItemsGrid();
				}
			});
			itemsGrid.add(cell);
		}

		itemsGrid.revalidate();
		itemsGrid.repaint();

		// fill in proper item names asynchronously for the tooltips
		if (!itemIds.isEmpty())
		{
			plugin.lookupItemNames(itemIds, names ->
			{
				java.awt.Component[] cells = itemsGrid.getComponents();
				for (int i = 0; i < cells.length && i < itemIds.size(); i++)
				{
					String name = names.get(itemIds.get(i));
					if (name != null)
					{
						((JLabel) cells[i]).setToolTipText(name + " - click to remove");
					}
				}
			});
		}
	}

	private void updateIconPreview()
	{
		if (selectedIconId > -1)
		{
			itemManager.getImage(selectedIconId).addTo(iconPreview);
		}
	}

	private void openColorPicker()
	{
		RuneliteColorPicker picker = plugin.getColorPickerManager().create(
			this, selectedColor, "Category color", true);
		picker.setLocationRelativeTo(this);
		picker.setOnColorChange(c ->
		{
			selectedColor = new Color(c.getRGB() & 0xFFFFFF);
			colorButton.setBackground(selectedColor);
		});
		picker.setVisible(true);
	}

	private void showLoginRequired()
	{
		JOptionPane.showMessageDialog(this,
			"You need to be logged in to search for items.",
			"Item search", JOptionPane.INFORMATION_MESSAGE);
	}

	private void save()
	{
		String name = nameField.getText().trim();
		boolean renamable = category == null || !category.isNewItems();

		if (renamable)
		{
			if (name.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Enter a name for the category.",
					"Missing name", JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (name.length() > CategoryManager.MAX_NAME_LENGTH)
			{
				name = name.substring(0, CategoryManager.MAX_NAME_LENGTH);
			}

			if (name.equalsIgnoreCase(CategoryManager.NEW_ITEMS_CATEGORY_NAME))
			{
				JOptionPane.showMessageDialog(this, "'" + CategoryManager.NEW_ITEMS_CATEGORY_NAME +
					"' is reserved for automatically collected items.", "Reserved name", JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (!categoryManager.isNameAvailable(name, category))
			{
				JOptionPane.showMessageDialog(this, "A category named '" + name + "' already exists.",
					"Duplicate name", JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

		Set<ItemKind> kinds = kindBoxes.entrySet().stream()
			.filter(e -> e.getValue().isSelected())
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());
		List<String> filters = Arrays.stream(filtersField.getText().split(","))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toList());

		if (category == null)
		{
			categoryManager.addCategory(name, selectedColor, selectedIconId, kinds, filters);
		}
		else
		{
			categoryManager.updateCategory(category, name, selectedColor, selectedIconId, kinds, filters);
		}

		// re-sort previous auto-placements so rule changes take effect immediately
		plugin.categoryRulesChanged();

		mainPanel.showListView();
	}
}
