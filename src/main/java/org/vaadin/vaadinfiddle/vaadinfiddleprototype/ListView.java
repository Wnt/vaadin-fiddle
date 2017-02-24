package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

public class ListView extends CustomComponent implements View {
	private VerticalLayout layout;
	private DockerClient dockerClient;
	private Grid<Container> grid;
	private DockerService dockerService;

	public ListView() {
		setSizeFull();

		layout = new VerticalLayout();
		setCompositionRoot(layout);
		layout.setSizeFull();

		dockerService = FiddleUi.getDockerservice();
		dockerClient = dockerService.getDockerClient();

		createTestButton();

		createList();
	}

	private void createTestButton() {
		Button button = new Button("Start a new instance");
		layout.addComponents(button);
		button.addClickListener(e -> {
			createNewContainer();
		});
	}

	private void createNewContainer() {
		CreateContainerResponse container = dockerService.createFiddleContainer();
		String id = container.getId();

		dockerService.startContainer(id);

		dockerService.runJetty(id, new WindowOutput());

		refreshList();
	}


	private void createList() {

		grid = new Grid<>();
		layout.addComponent(grid);
		layout.setExpandRatio(grid, 1);
		grid.setSizeFull();

		refreshList();

		grid.addColumn(c -> {
			return String.join(", ", c.getNames());
		}).setCaption("Name");
		grid.addColumn(Container::getImage).setCaption("Image");
		grid.addColumn(Container::getStatus).setCaption("Status");
		grid.addColumn(c -> {
			ContainerPort[] ports = c.getPorts();
			ArrayList<String> portStrings = new ArrayList<>();
			for (ContainerPort p : ports) {
				portStrings.add(p.getIp() + ":" + p.getPublicPort() + " -> " + p.getPrivatePort() + "/" + p.getType());
			}
			return String.join("", portStrings);
			// return StringUtils.join(portStrings, ", ");
		}).setCaption("Ports");

		grid.addColumn(c -> {
			ContainerNetworkSettings networkSettings = c.getNetworkSettings();
			Map<String, ContainerNetwork> networks = networkSettings.getNetworks();

			ArrayList<String> ips = new ArrayList<>();
			for (String networkId : networks.keySet()) {
				ContainerNetwork containerNetwork = networks.get(networkId);

				ips.add(containerNetwork.getIpAddress());

			}
			return String.join(",", ips);

		}).setCaption("IP");

		grid.addColumn(((ValueProvider<Container, String>) c -> {
			String containerPage = "#!container/" + c.getId();
			return "<a href=\"" + containerPage + "\" title=\"Inspect container\">"
					+ VaadinIcons.EXTERNAL_LINK.getHtml() + "</a>";
		}), new HtmlRenderer()).setCaption("Tools");

		grid.addColumn(Container::getCommand).setCaption("Command");
		grid.addColumn(c -> {
			ContainerNetworkSettings networkSettings = c.getNetworkSettings();

			InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(c.getId()).exec();

			for (Mount mount : containerInfo.getMounts()) {
				String target = mount.getDestination().getPath();
				if ("/webapp/fiddleapp".equals(target)) {
					return mount.getSource();
				}

			}
			return "";
		}).setCaption("Datadir");
	}

	private void refreshList() {
		List<Container> containerList = dockerClient.listContainersCmd().withStatusFilter("running").exec();
		grid.setItems(containerList);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		Page.getCurrent().setTitle("Container list - VaadinFiddle");

	}

}
