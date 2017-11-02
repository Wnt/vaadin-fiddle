package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.apache.commons.lang.StringUtils;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.ContainerView;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.CreatorView;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.ForkView;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.ListView;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.view.PreviewView;

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
		CREATOR(""), LIST("list"), CONTAINER("container"), FORK("fork"), PREVIEW("preview");
		private final String id;

		ViewIds(final String s) {
			id = s;
		}

		public String toString() {
			return id;
		}
	};

	final private static DockerService dockerService = new DockerService();

	@Override
	protected void init(VaadinRequest vaadinRequest) {

		Navigator navi = new Navigator(this, this);
		navi.addView(ViewIds.CREATOR + "", CreatorView.class);
		navi.addView(ViewIds.LIST + "", ListView.class);
		navi.addView(ViewIds.CONTAINER + "", new ContainerView());
		navi.addView(ViewIds.FORK + "", ForkView.class);
		navi.addView(ViewIds.PREVIEW + "", PreviewView.class);

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

								int dirs = StringUtils.countMatches(pathInfo, "/");
								String relativePathToContextRoot = String.join("",
										Collections.nCopies(dirs - 1, "../"));
								response.getDocument().head()
										.append("<meta property=\"og:title\" content=\"Hello world!\" />\n"
												+ "<meta property=\"og:image\" content=\"" + relativePathToContextRoot
												+ "preview-image/" + imgFilename + "\" />");

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
		FiddleUi.getDockerservice().unregisterUI(this);
	}
}
