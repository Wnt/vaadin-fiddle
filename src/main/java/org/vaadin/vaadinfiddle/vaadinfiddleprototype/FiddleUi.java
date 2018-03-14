package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.ContainerView;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
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
@PushStateNavigation
public class FiddleUi extends UI {

	public static enum ViewIds {
		CONTAINER("");
		private final String id;

		ViewIds(final String s) {
			id = s;
		}

		public String toString() {
			return id;
		}
	};

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		Navigator navi = new Navigator(this, this);
		navi.addView(ViewIds.CONTAINER + "", new ContainerView());

		navi.addViewChangeListener(e -> {

			Collection<Window> windows = new ArrayList<>(UI.getCurrent().getWindows());
			for (Window w : windows) {
				w.close();
			}
			return true;
		});
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

		@Override
		protected void servletInitialized() throws ServletException {
			super.servletInitialized();
			getService()
					.addSessionInitListener(event -> event.getSession().addBootstrapListener(new BootstrapListener() {

						@Override
						public void modifyBootstrapPage(BootstrapPageResponse response) {
							String pathInfo = response.getRequest().getPathInfo();

							String containerViewPrefix = "/container/";
							if (pathInfo.startsWith(containerViewPrefix)) {

								String imgFilename = pathInfo.substring(containerViewPrefix.length()).replace('/', '_')
										+ ".png";
								String filename = pathInfo.substring(pathInfo.lastIndexOf('/') + 1);

								// TODO look up from enviroment
								String contextRootUrl = "https://vaadinfiddle.com/editor";
								response.getDocument().head()
										.append("<meta property=\"og:url\" content=\"" + contextRootUrl + pathInfo
												+ "\" />\n" + "<meta property=\"og:type\" content=\"website\" />\n"
												+ "<meta property=\"og:title\" content=\"" + filename
												+ " on VaadinFiddle\" />\n" + "<meta property=\"og:image\" content=\""
												+ contextRootUrl + "/preview-image/" + imgFilename + "\" />\n"
												+ "<meta property=\"og:image:width\" content=\"1067\" />\n"
												+ "<meta property=\"og:image:height\" content=\"473\" />");

							}

						}

						@Override
						public void modifyBootstrapFragment(BootstrapFragmentResponse response) {
							// TODO Auto-generated method stub

						}
					}));
		}
	}

	@Override
	public void detach() {
		super.detach();
	}
}
