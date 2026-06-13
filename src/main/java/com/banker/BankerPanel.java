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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
class BankerPanel extends PluginPanel
{
	private static final ImageIcon ADD_ICON;
	private static final ImageIcon ADD_HOVER_ICON;

	static
	{
		final BufferedImage addImage = ImageUtil.loadImageResource(BankerPanel.class, "add_icon.png");
		ADD_ICON = new ImageIcon(addImage);
		ADD_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(addImage, 0.53f));
	}

	private final BankerPlugin plugin;
	private final CategoryManager categoryManager;
	private final TemplateManager templateManager;
	private final ItemManager itemManager;

	private final JPanel listView;
	private final JPanel categoryList = new JPanel(new GridBagLayout());

	BankerPanel(BankerPlugin plugin, CategoryManager categoryManager,
		TemplateManager templateManager, ItemManager itemManager)
	{
		super(false);
		this.plugin = plugin;
		this.categoryManager = categoryManager;
		this.templateManager = templateManager;
		this.itemManager = itemManager;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		listView = buildListView();
		add(listView, BorderLayout.CENTER);
		refreshList();
	}

	private JPanel buildListView()
	{
		JPanel container = new JPanel(new BorderLayout(0, 8));
		container.setBorder(new EmptyBorder(10, 10, 10, 10));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// header: title + add button + hint
		JPanel north = new JPanel(new BorderLayout(0, 6));
		north.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Banker");
		title.setForeground(java.awt.Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		titleRow.add(title, BorderLayout.WEST);

		JLabel addButton = new JLabel(ADD_ICON);
		addButton.setToolTipText("Add a new category");
		addButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				openEditor(null);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				addButton.setIcon(ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				addButton.setIcon(ADD_ICON);
			}
		});
		titleRow.add(addButton, BorderLayout.EAST);
		north.add(titleRow, BorderLayout.NORTH);

		JLabel hint = new JLabel("<html>Right-click a bank item to assign it to a category. " +
			"Click a category below to view only its items in the bank. Drag rows to reorder the tabs.</html>");
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		north.add(hint, BorderLayout.CENTER);
		container.add(north, BorderLayout.NORTH);

		// category list
		categoryList.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(categoryList, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listWrapper,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		container.add(scrollPane, BorderLayout.CENTER);

		// footer: auto-assign + templates / import / export
		JPanel footer = new JPanel(new BorderLayout(0, 6));
		footer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton autoAssignButton = new JButton("Auto-assign bank items");
		autoAssignButton.setToolTipText("<html>Sort bank items into categories using each category's<br>" +
			"item types and name filters. Open your bank first.</html>");
		autoAssignButton.addActionListener(e -> promptAutoAssign());
		footer.add(autoAssignButton, BorderLayout.NORTH);

		JPanel footerRow = new JPanel(new GridLayout(1, 3, 6, 0));
		footerRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton templatesButton = new JButton("Templates");
		templatesButton.setToolTipText("Start from a ready-made set of categories");
		templatesButton.addActionListener(e -> showTemplateMenu(templatesButton));
		footerRow.add(templatesButton);

		JButton importButton = new JButton("Import");
		importButton.setToolTipText("Import categories from a file or a shared setup code");
		importButton.addActionListener(e ->
		{
			JPopupMenu menu = new JPopupMenu();
			javax.swing.JMenuItem fromFile = new javax.swing.JMenuItem("From fileÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦");
			fromFile.addActionListener(ev -> importCategories());
			menu.add(fromFile);
			javax.swing.JMenuItem fromClipboard = new javax.swing.JMenuItem("From clipboard code");
			fromClipboard.addActionListener(ev -> importFromClipboard());
			menu.add(fromClipboard);
			menu.show(importButton, 0, importButton.getHeight());
		});
		footerRow.add(importButton);

		JButton exportButton = new JButton("Export");
		exportButton.setToolTipText("Export categories to a file, or copy a setup code to share");
		exportButton.addActionListener(e ->
		{
			JPopupMenu menu = new JPopupMenu();
			javax.swing.JMenuItem toFile = new javax.swing.JMenuItem("To fileÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦");
			toFile.addActionListener(ev -> exportCategories());
			menu.add(toFile);
			javax.swing.JMenuItem toClipboard = new javax.swing.JMenuItem("Copy code to clipboard");
			toClipboard.addActionListener(ev -> exportToClipboard());
			menu.add(toClipboard);
			menu.show(exportButton, 0, exportButton.getHeight());
		});
		footerRow.add(exportButton);

		footer.add(footerRow, BorderLayout.SOUTH);
		container.add(footer, BorderLayout.SOUTH);
		return container;
	}

	private void promptAutoAssign()
	{
		String[] options = {"New items only", "Re-sort everything", "Cancel"};
		int choice = JOptionPane.showOptionDialog(this,
			"Sort only unassigned items into categories,\n" +
				"or re-sort every bank item to match the current rules?\n\n" +
				"Re-sorting moves items between categories, including ones\n" +
				"you assigned by hand.",
			"Auto-assign", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, options, options[0]);

		if (choice == 0)
		{
			plugin.autoAssignBankItems(false, true);
		}
		else if (choice == 1)
		{
			plugin.autoAssignBankItems(true, true);
		}
	}

	void showAutoAssignResult(int assigned, java.util.List<Integer> unmatched)
	{
		if (assigned < 0)
		{
			JOptionPane.showMessageDialog(this,
				"Open your bank first so the plugin can see your items.",
				"Auto-assign", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		String message = assigned == 0
			? "No unassigned items could be matched to your categories."
			: "Assigned " + assigned + (assigned == 1 ? " item" : " items") + " to matching categories.";

		if (unmatched.isEmpty())
		{
			JOptionPane.showMessageDialog(this, message, "Auto-assign", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		int choice = JOptionPane.showConfirmDialog(this,
			message + "\n\n" + unmatched.size() + (unmatched.size() == 1 ? " item" : " items") +
				" didn't match any category.\nMove them into a catch-all 'Other' category?\n" +
				"(Tip: tick 'Everything else' on one of your categories to do this automatically.)",
			"Auto-assign", JOptionPane.YES_NO_OPTION);
		if (choice == JOptionPane.YES_OPTION)
		{
			plugin.assignToCatchAll(unmatched);
		}
	}

	void refreshList()
	{
		categoryList.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 6, 0);

		List<CategoryListItem> rows = new ArrayList<>();
		for (Category category : categoryManager.getCategories())
		{
			CategoryListItem row = new CategoryListItem(plugin, categoryManager, itemManager, this, category);
			rows.add(row);
			categoryList.add(row, c);
			c.gridy++;
		}

		JLabel unassignedStatus = new JLabel("open your bank to count");
		categoryList.add(buildUnassignedRow(unassignedStatus), c);

		categoryList.revalidate();
		categoryList.repaint();

		// fill in bank values and the unassigned count asynchronously
		plugin.computeBankStats((values, unassignedCount) ->
		{
			if (values != null)
			{
				for (CategoryListItem row : rows)
				{
					Long value = values.get(row.getCategory());
					if (value != null)
					{
						row.setBankValue(value);
					}
				}
			}

			if (unassignedCount >= 0)
			{
				unassignedStatus.setText(unassignedCount + (unassignedCount == 1 ? " item" : " items") + " in bank"
					+ (plugin.isViewingUnassigned() ? " ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· viewing" : ""));
			}
		});
	}

	private JPanel buildUnassignedRow(JLabel statusLabel)
	{
		boolean viewing = plugin.isViewingUnassigned();

		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBorder(new javax.swing.border.CompoundBorder(
			javax.swing.BorderFactory.createMatteBorder(0, 4, 0, 0, ColorScheme.LIGHT_GRAY_COLOR),
			new EmptyBorder(6, 6, 6, 6)));
		java.awt.Color base = viewing ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
		row.setBackground(base);
		row.setToolTipText(viewing
			? "Viewing unassigned items - click to show all items again"
			: "Click to show only items that aren't in any category");

		JPanel textPanel = new JPanel(new GridLayout(2, 1));
		textPanel.setOpaque(false);

		JLabel nameLabel = new JLabel("Unassigned");
		nameLabel.setForeground(java.awt.Color.WHITE);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		textPanel.add(nameLabel);

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(viewing ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
		textPanel.add(statusLabel);
		row.add(textPanel, BorderLayout.CENTER);

		MouseAdapter listener = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					plugin.viewUnassigned();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(base);
			}
		};
		row.addMouseListener(listener);
		nameLabel.addMouseListener(listener);
		statusLabel.addMouseListener(listener);
		textPanel.addMouseListener(listener);

		return row;
	}

	private void importFromClipboard()
	{
		String data;
		try
		{
			data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			data = null;
		}

		if (data == null || !data.trim().startsWith(CategoryManager.SHARE_CODE_PREFIX))
		{
			JOptionPane.showMessageDialog(this,
				"Your clipboard doesn't contain a Banker setup code.",
				"Import failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
			"Importing replaces your current categories and item assignments. Continue?",
			"Import categories", JOptionPane.OK_CANCEL_OPTION);
		if (confirm != JOptionPane.OK_OPTION)
		{
			return;
		}

		if (!categoryManager.importShareCode(data))
		{
			JOptionPane.showMessageDialog(this,
				"Couldn't import that setup code - it looks damaged or incomplete.",
				"Import failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void exportToClipboard()
	{
		String code = categoryManager.exportShareCode();
		if (code == null)
		{
			JOptionPane.showMessageDialog(this, "Couldn't build a setup code.",
				"Export failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
		JOptionPane.showMessageDialog(this,
			"Setup code copied to your clipboard - paste it anywhere to share.",
			"Export", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Move a category row to the list position under the given y coordinate
	 * (in categoryList space). Used by row drag-and-drop.
	 */
	void reorderCategory(Category category, int dropY)
	{
		// final position = number of *other* rows whose midpoint is above the drop point
		int index = 0;
		for (java.awt.Component row : categoryList.getComponents())
		{
			if (!(row instanceof CategoryListItem) || ((CategoryListItem) row).getCategory() == category)
			{
				continue;
			}
			if (dropY > row.getY() + row.getHeight() / 2)
			{
				index++;
			}
		}
		categoryManager.moveCategory(category, index);
	}

	void openEditor(Category category)
	{
		removeAll();
		add(new CategoryEditorPanel(plugin, categoryManager, this, itemManager, category), BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	void showListView()
	{
		removeAll();
		add(listView, BorderLayout.CENTER);
		refreshList();
		revalidate();
		repaint();
	}

	private void showTemplateMenu(JButton anchor)
	{
		JPopupMenu menu = new JPopupMenu();
		for (String name : templateManager.getTemplateNames())
		{
			javax.swing.JMenuItem item = new javax.swing.JMenuItem(name);
			item.addActionListener(e -> applyTemplate(name));
			menu.add(item);
		}
		menu.show(anchor, 0, anchor.getHeight());
	}

	private void applyTemplate(String name)
	{
		int confirm = JOptionPane.showConfirmDialog(this,
			"Apply the '" + name + "' template?\nThis replaces your current categories and item assignments\n" +
				"(items collected in New Items are kept).",
			"Apply Template", JOptionPane.OK_CANCEL_OPTION);
		if (confirm == JOptionPane.OK_OPTION)
		{
			categoryManager.replaceCategories(templateManager.createTemplate(name));
		}
	}

	private void importCategories()
	{
		JFileChooser fileChooser = new JFileChooser(RuneLite.RUNELITE_DIR);
		fileChooser.setDialogTitle("Import categories");
		fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this,
			"Importing replaces your current categories and item assignments. Continue?",
			"Import categories", JOptionPane.OK_CANCEL_OPTION);
		if (confirm != JOptionPane.OK_OPTION)
		{
			return;
		}

		File file = fileChooser.getSelectedFile();
		boolean ok = false;
		try
		{
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			ok = categoryManager.importJson(json);
		}
		catch (IOException e)
		{
			log.warn("Failed to read {}", file, e);
		}

		if (!ok)
		{
			JOptionPane.showMessageDialog(this,
				"Couldn't import categories from that file - it doesn't look like a Banker export.",
				"Import failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void exportCategories()
	{
		JFileChooser fileChooser = new JFileChooser(RuneLite.RUNELITE_DIR);
		fileChooser.setDialogTitle("Export categories");
		fileChooser.setSelectedFile(new File("bank-organizer-categories.json"));
		fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File file = fileChooser.getSelectedFile();
		if (!file.getName().toLowerCase().endsWith(".json"))
		{
			file = new File(file.getParentFile(), file.getName() + ".json");
		}

		try
		{
			Files.write(file.toPath(), categoryManager.exportJson().getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("Failed to write {}", file, e);
			JOptionPane.showMessageDialog(this, "Couldn't write to that location.",
				"Export failed", JOptionPane.ERROR_MESSAGE);
		}
	}
}
