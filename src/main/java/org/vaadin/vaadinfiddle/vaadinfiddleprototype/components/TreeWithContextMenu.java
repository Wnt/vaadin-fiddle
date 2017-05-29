package org.vaadin.vaadinfiddle.vaadinfiddleprototype.components;

import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.ui.Tree;
import com.vaadin.ui.TreeGrid;

public class TreeWithContextMenu<T> extends Tree<T> {
		private GridContextMenu<T> gridContextMenu;

		public GridContextMenu<T> getContextMenu() {
			if (gridContextMenu == null) {
				TreeGrid<T> treegrid = (TreeGrid) getCompositionRoot();
				gridContextMenu = new GridContextMenu<>(treegrid);
			}
			return gridContextMenu;
		}
	}