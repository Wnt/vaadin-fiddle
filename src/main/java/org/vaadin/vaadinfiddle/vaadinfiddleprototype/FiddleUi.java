package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

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
@Widgetset("com.vaadin.v7.Vaadin7WidgetSet")
public class FiddleUi extends UI {

	final private static DockerService dockerService = new DockerService();

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		Navigator navi = new Navigator(this, this);
		navi.addView("", CreatorView.class);
		navi.addView("list", ListView.class);
		navi.addView("container", ContainerView.class);
		navi.addView("fork", ForkView.class);

		navi.addViewChangeListener(e -> {

			Collection<Window> windows = new ArrayList<>(UI.getCurrent().getWindows());
			for (Window w : windows) {
				w.close();
			}
			return true;
		});
	}

	public static DockerService getDockerservice() {
		return dockerService;
	}

	@WebServlet(urlPatterns = { "/*" }, name = "VaadinFiddleUiServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = FiddleUi.class, productionMode = false)
	public static class VaadinFiddleUiServlet extends VaadinServlet {
		
		@Override
		protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
				throws ServiceException {

			VaadinServletService service = new FiddleServletService(this, deploymentConfiguration);
			service.init();
			return service;

		}
	}
}
