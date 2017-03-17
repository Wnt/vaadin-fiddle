package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.Page;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public class WindowOutput extends OutputStream {
	private Layout consoleOutput;
	private StringBuilder row = new StringBuilder();
	private Window window;
	private List<JettyStartListener> jettyStartListeners = new ArrayList<>();
	private Panel scroller;

	public WindowOutput() {
		consoleOutput = new CssLayout();
		scroller = new Panel(consoleOutput);
		scroller.setSizeFull();
		window = new Window("Start output", scroller);
		window.setWidth("500px");
		window.setHeight("250px");
		
		consoleOutput.setWidth("100%");
		consoleOutput.addStyleName("console-msg-container");

		UI.getCurrent().addWindow(window);

		window.setPositionX(30);
		window.setPositionY(Page.getCurrent().getBrowserWindowHeight() - 250 - 30);
		
		window.addStyleName("console-output-window");

	}

	@Override
	public void write(int b) throws IOException {
		int[] bytes = { b };
		String s = new String(bytes, 0, bytes.length);

		row.append(s);
		if (s.contains("\n")) {
			String str = row.toString();
			if (str.contains("[INFO] Started Jetty Server")) {
				for (JettyStartListener l : jettyStartListeners) {
					l.jettyStarted();
				}
			}
			row = new StringBuilder();
			if (consoleOutput.isAttached()) {
				consoleOutput.getUI().access(new Runnable() {

					@Override
					public void run() {
						Label c = new Label(str);
						consoleOutput.addComponent(c);
						scroller.setScrollTop(Integer.MAX_VALUE);
					}
				});
			}
		}
	}
	public void addJettyStartListener(JettyStartListener l ) {
		jettyStartListeners.add(l);
	}
	
	public interface JettyStartListener {
		void jettyStarted();
	}

}
