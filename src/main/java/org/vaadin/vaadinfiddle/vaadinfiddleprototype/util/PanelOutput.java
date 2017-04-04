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

public class PanelOutput extends OutputStream {
	private Layout consoleOutput;
	private StringBuilder row = new StringBuilder();
	private List<JettyStartListener> jettyStartListeners = new ArrayList<>();
	private List<FirstMessageReceivedListener> firstMessageReceivedListeners = new ArrayList<>();
	private final Panel outputPanel;
	private boolean firstMessageReceived = false;

	public PanelOutput() {
		consoleOutput = new CssLayout();
		outputPanel = new Panel(consoleOutput);
		getOutputPanel().setSizeFull();
		
		consoleOutput.setWidth("100%");
		consoleOutput.addStyleName("console-msg-container");

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
						getOutputPanel().setScrollTop(Integer.MAX_VALUE);
					}
				});
			}
			if (!firstMessageReceived) {
				firstMessageReceived = true;
				for (FirstMessageReceivedListener l : firstMessageReceivedListeners) {
					l.messageReceived();
				}
			}
		}
	}
	public void addJettyStartListener(JettyStartListener l ) {
		jettyStartListeners.add(l);
	}
	
	public Panel getOutputPanel() {
		return outputPanel;
	}

	public void addFirstLineReceivedListener(FirstMessageReceivedListener l) {
		firstMessageReceivedListeners.add(l);
		
	}

	public interface JettyStartListener {
		void jettyStarted();
	}

	public interface FirstMessageReceivedListener {
		void messageReceived();
	}

}
