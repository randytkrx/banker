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
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

class CategoryListItem extends JPanel
{
	private static final ImageIcon EDIT_ICON;
	private static final ImageIcon EDIT_HOVER_ICON;
	private static final ImageIcon DELETE_ICON;
	private static final ImageIcon DELETE_HOVER_ICON;

	static
	{
		final BufferedImage editImg = ImageUtil.loadImageResource(CategoryListItem.class, "edit_icon.png");
		EDIT_ICON = new ImageIcon(editImg);
		EDIT_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(editImg, 0.53f));

		final BufferedImage deleteImg = ImageUtil.loadImageResource(CategoryListItem.class, "delete_icon.png");
		DELETE_ICON = new ImageIcon(deleteImg);
		DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, 0.53f));
	}

	private final BankerPlugin plugin;
	@lombok.Getter
	private final Category category;
	private final boolean viewing;
	private final JLabel statusLabel;
	private final String baseStatus;

	CategoryListItem(BankerPlugin plugin, CategoryManager categoryManager, ItemManager itemManager,
		BankerPanel panel, Category category)
	{
		this.plugin = plugin;
		this.category = category;
		this.viewing = plugin.isViewing(category);

		setLayout(new BorderLayout(8, 0));
		setBorder(new CompoundBorder(
			BorderFactory.createMatteBorder(0, 4, 0, 0, category.getAwtColor()),
			new EmptyBorder(6, 6, 6, 6)));
		setBackground(rowColor());
		setToolTipText(viewing
			? "Viewing in bank - click to show all items again"
			: "Click to show only this category in the bank");

		// item icon
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(36, 32));
		iconLabel.setHorizontalAlignment(JLabel.CENTER);
		if (category.getIconItemId() > -1)
		{
			itemManager.getImage(category.getIconItemId()).addTo(iconLabel);
		}
		add(iconLabel, BorderLayout.WEST);

		// name + status
		JPanel textPanel = new JPanel(new GridLayout(2, 1));
		textPanel.setOpaque(false);

		JLabel nameLabel = new JLabel(category.getName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		textPanel.add(nameLabel);

		int count = category.getItemIds().size();
		baseStatus = count + (count == 1 ? " item" : " items") + (viewing ? " ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· viewing" : "");
		statusLabel = new JLabel(baseStatus);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(viewing ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
		textPanel.add(statusLabel);
		add(textPanel, BorderLayout.CENTER);

		// edit / delete controls
		JPanel controls = new JPanel(new GridLayout(1, 0, 6, 0));
		controls.setOpaque(false);

		JLabel editLabel = new JLabel(EDIT_ICON);
		editLabel.setToolTipText("Edit category");
		editLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					panel.openEditor(category);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				editLabel.setIcon(EDIT_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				editLabel.setIcon(EDIT_ICON);
			}
		});
		controls.add(editLabel);

		if (!category.isNewItems() && !category.isMainTab())
		{
			JLabel deleteLabel = new JLabel(DELETE_ICON);
			deleteLabel.setToolTipText("Delete category");
			deleteLabel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (e.getButton() == MouseEvent.BUTTON1 && confirmDelete())
					{
						categoryManager.removeCategory(category);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					deleteLabel.setIcon(DELETE_HOVER_ICON);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					deleteLabel.setIcon(DELETE_ICON);
				}
			});
			controls.add(deleteLabel);
		}
		add(controls, BorderLayout.EAST);

		// view-toggle, drag-reorder, hover and context menu on the row and its
		// passive children. a click toggles the bank view; dragging past a small
		// threshold reorders the category instead.
		MouseAdapter rowListener = new MouseAdapter()
		{
			private java.awt.Point pressPoint;
			private boolean dragging;

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					pressPoint = e.getPoint();
					dragging = false;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (pressPoint != null && !dragging && pressPoint.distance(e.getPoint()) > 5)
				{
					dragging = true;
					setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
					setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				setCursor(java.awt.Cursor.getDefaultCursor());
				if (e.getButton() != MouseEvent.BUTTON1 || pressPoint == null)
				{
					return;
				}
				pressPoint = null;

				if (dragging)
				{
					dragging = false;
					java.awt.Point drop = javax.swing.SwingUtilities.convertPoint(
						e.getComponent(), e.getPoint(), CategoryListItem.this.getParent());
					panel.reorderCategory(category, drop.y);
				}
				else
				{
					plugin.viewCategory(category);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (!dragging)
				{
					setBackground(rowColor());
				}
			}
		};

		JPopupMenu popup = buildPopupMenu(categoryManager, panel);
		for (JComponent component : new JComponent[]{this, iconLabel, textPanel, nameLabel, statusLabel})
		{
			component.addMouseListener(rowListener);
			component.addMouseMotionListener(rowListener);
			component.setComponentPopupMenu(popup);
		}
	}

	/**
	 * Append the GE value of this category's items currently in the bank.
	 */
	void setBankValue(long value)
	{
		statusLabel.setText(baseStatus + " ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· " +
			net.runelite.client.util.QuantityFormatter.quantityToStackSize(value) + " gp");
	}

	private Color rowColor()
	{
		return viewing ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR;
	}

	private JPopupMenu buildPopupMenu(CategoryManager categoryManager, BankerPanel panel)
	{
		JPopupMenu popup = new JPopupMenu();

		JMenuItem view = new JMenuItem(viewing ? "Stop viewing in bank" : "View in bank");
		view.addActionListener(e -> plugin.viewCategory(category));
		popup.add(view);

		JMenuItem edit = new JMenuItem("Edit");
		edit.addActionListener(e -> panel.openEditor(category));
		popup.add(edit);

		if (!category.isMainTab())
		{
			JMenuItem moveUp = new JMenuItem("Move up");
			moveUp.addActionListener(e ->
				categoryManager.moveCategory(category, categoryManager.getCategories().indexOf(category) - 1));
			popup.add(moveUp);

			JMenuItem moveDown = new JMenuItem("Move down");
			moveDown.addActionListener(e ->
				categoryManager.moveCategory(category, categoryManager.getCategories().indexOf(category) + 1));
			popup.add(moveDown);
		}

		JMenuItem clear = new JMenuItem("Clear items");
		clear.addActionListener(e ->
		{
			int confirm = JOptionPane.showConfirmDialog(this,
				"Remove all " + category.getItemIds().size() + " items from '" + category.getName() + "'?",
				"Clear items", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION)
			{
				categoryManager.clearItems(category);
			}
		});
		popup.add(clear);

		if (!category.isNewItems())
		{
			JMenuItem delete = new JMenuItem("Delete");
			delete.addActionListener(e ->
			{
				if (confirmDelete())
				{
					categoryManager.removeCategory(category);
				}
			});
			popup.add(delete);
		}

		return popup;
	}

	private boolean confirmDelete()
	{
		return JOptionPane.showConfirmDialog(this,
			"Delete the '" + category.getName() + "' category?",
			"Confirm deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}
}
