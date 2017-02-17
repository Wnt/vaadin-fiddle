package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * This UI is the application entry point. A UI may either represent a browser
 * window (or tab) or some part of a html page where a Vaadin application is
 * embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is
 * intended to be overridden to add component to the user interface and
 * initialize non-component functionality.
 */
@Theme("vaadin-fiddle")
@Push
public class FiddleUi extends UI {

	final private static DockerService dockerService = new DockerService();
	
	@Override
	protected void init(VaadinRequest vaadinRequest) {
		
		Navigator navi = new Navigator(this,  this);
		navi.addView("", ListView.class);
		navi.addView("container", ContainerView.class);

	}
	
	public static DockerService getDockerservice() {
		return dockerService;
	}

	@WebServlet(urlPatterns = "/*", name = "VaadinFiddleUiServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = FiddleUi.class, productionMode = false)
	public static class VaadinFiddleUiServlet extends VaadinServlet {
	}
}
