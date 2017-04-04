package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import com.vaadin.server.Page;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public class WindowOutput extends PanelOutput {

	private Window window;

	public WindowOutput() {
		window = new Window("Start output", getOutputPanel());
		window.setWidth("500px");
		window.setHeight("250px");
		
		UI.getCurrent().addWindow(window);

		window.setPositionX(30);
		window.setPositionY(Page.getCurrent().getBrowserWindowHeight() - 250 - 30);
		
		window.addStyleName("console-output-window");

	}

}
