package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

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
public class VaadinFiddleUi extends UI {

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();

		final TextField name = new TextField();
		name.setCaption("Type your name here:");

		Button button = new Button("Click Me");
		button.addClickListener(e -> {
			Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
			DockerClient dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
			
			List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
			for (Container c : containers) {
				String status = c.getStatus();
				if (status.startsWith("Exited")) {
					continue;
				}
				layout.addComponent(new Label(c.toString()));
			}			
		});

		layout.addComponents(name, button);

		setContent(layout);
	}

	@WebServlet(urlPatterns = "/*", name = "VaadinFiddleUiServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = VaadinFiddleUi.class, productionMode = false)
	public static class VaadinFiddleUiServlet extends VaadinServlet {
	}
}
