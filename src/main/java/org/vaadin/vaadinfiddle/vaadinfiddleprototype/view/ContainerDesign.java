package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.declarative.Design;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.components.TreeWithContextMenu;

/** 
 * !! DO NOT EDIT THIS FILE !!
 * 
 * This class is generated by Vaadin Designer and will be overwritten.
 * 
 * Please make a subclass with logic and additional interfaces as needed,
 * e.g class LoginView extends LoginDesign implements View { }
 */
@DesignRoot
@AutoGenerated
@SuppressWarnings("serial")
public class ContainerDesign extends VerticalLayout {
	protected Button saveButton;
	protected Button forkButton;
	protected Button newButton;
	protected TreeWithContextMenu tree;
	protected HorizontalSplitPanel mainAreaAndFiddleResult;
	protected VerticalSplitPanel editorTabsAndConsole;
	protected TabSheet editorTabs;

	public ContainerDesign() {
		Design.read(this);
	}
}
