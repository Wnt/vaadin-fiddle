package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.DockerService;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi.ViewIds;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.HtmlRenderer;

public class ListView extends CustomComponent implements View {
	private VerticalLayout layout;
	private Grid<Container> grid;
	private DockerService dockerService;

	public ListView() {
		setSizeFull();

		layout = new VerticalLayout();
		setCompositionRoot(layout);
		layout.setSizeFull();

		dockerService = FiddleUi.getDockerservice();

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

		refreshList();
	}

	private void createList() {

		grid = new Grid<>();
		layout.addComponent(grid);
		layout.setExpandRatio(grid, 1);
		grid.setSizeFull();

		refreshList();

		createColumnName();
		createColumnImage();
		createColumnnStatus();
		createColumnPorts();

		createColumnIp();

		createColumnTools();

		createColumnCommand();
		createColumnDatadir();
	}

	private void createColumnDatadir() {
		grid.addColumn(c -> {
			return FiddleUi.getDockerservice().getDatadirById(c.getId());
		}).setCaption("Datadir");
	}

	private void createColumnCommand() {
		grid.addColumn(Container::getCommand).setCaption("Command");
	}

	private void createColumnTools() {
		grid.addColumn(((ValueProvider<Container, String>) c -> {

			String currentLocation = Page.getCurrent().getLocation().toString();
			int deploymentPathEndIdx = currentLocation.indexOf(ViewIds.LIST.toString());
			String deploymentURL = currentLocation.substring(0, deploymentPathEndIdx);

			String containerPage = deploymentURL + "container/" + c.getId();
			return "<a href=\"" + containerPage + "\" title=\"Inspect container\">"
					+ VaadinIcons.EXTERNAL_LINK.getHtml() + "</a>";
		}), new HtmlRenderer()).setCaption("Tools");
	}

	private void createColumnIp() {
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
	}

	private void createColumnPorts() {
		grid.addColumn(c -> {
			ContainerPort[] ports = c.getPorts();
			ArrayList<String> portStrings = new ArrayList<>();
			for (ContainerPort p : ports) {
				portStrings.add(p.getIp() + ":" + p.getPublicPort() + " -> " + p.getPrivatePort() + "/" + p.getType());
			}
			return String.join("", portStrings);
			// return StringUtils.join(portStrings, ", ");
		}).setCaption("Ports");
	}

	private void createColumnnStatus() {
		grid.addColumn(Container::getStatus).setCaption("Status");
	}

	private void createColumnImage() {
		grid.addColumn(Container::getImage).setCaption("Image");
	}

	private void createColumnName() {
		grid.addColumn(c -> {
			return String.join(", ", c.getNames());
		}).setCaption("Name");
	}

	private void refreshList() {
		List<Container> containerList = dockerService.getContainers();
		grid.setItems(containerList);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		Page.getCurrent().setTitle("Container list - VaadinFiddle");

	}

}
