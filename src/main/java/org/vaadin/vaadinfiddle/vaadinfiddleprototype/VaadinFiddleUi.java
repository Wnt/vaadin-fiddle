package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

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
public class VaadinFiddleUi extends UI {

	private Label c;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();

		Button button = new Button("Click Me");
		layout.addComponents(button);
		button.addClickListener(e -> {
			Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
			DockerClient dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();

			Volume volume = new Volume("/webapp/fiddleapp");
//			Bind bind = new Bind("/home/jonni/vaadin_fiddles/test01", volume);
			CreateContainerResponse container = dockerClient.createContainerCmd("vaadin-stub")
					.withVolumes(volume)
					.withCmd("bash")
					.withTty(true)
//					 .withBinds(bind)
					.exec();
			String id = container.getId();

			dockerClient.startContainerCmd(id).exec();

			InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();
			Label label = new Label("" + containerInfo.getName());
			label.addStyleName(ValoTheme.LABEL_H1);
			layout.addComponent(label);
			Map<String, ContainerNetwork> networks = containerInfo.getNetworkSettings().getNetworks();
			for (String networkId : networks.keySet()) {
				ContainerNetwork containerNetwork = networks.get(networkId);
				
				layout.addComponent(new Label(containerNetwork.getIpAddress() + " @ " + containerNetwork.getNetworkID()));

			}
			for (Mount mount : containerInfo.getMounts()) {
				layout.addComponent(new Label(mount.getSource() + " -> " + mount.getDestination().getPath()));

			}

			ExecCreateCmdResponse cmd = dockerClient
					.execCreateCmd(id)
					.withCmd("su", "vaadin", "-c", "cd /webapp/fiddleapp; mvn jetty:run")
					.withAttachStdout(true)
					.exec();
			
	        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
	        
			ExecStartResultCallback start = dockerClient.execStartCmd(cmd.getId())
			        .withDetach(false)
			        .withTty(true)
			        .exec(new ExecStartResultCallback(System.out, System.err));

		});

		c = new Label("cmd output");

		layout.addComponent(c);

		setContent(layout);
	}

	@WebServlet(urlPatterns = "/*", name = "VaadinFiddleUiServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = VaadinFiddleUi.class, productionMode = false)
	public static class VaadinFiddleUiServlet extends VaadinServlet {
	}
}
