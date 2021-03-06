package org.vaadin.vaadinfiddle.vaadinfiddleprototype.data;

import java.io.File;
import java.util.List;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

public class FiddleContainer {

	final private InspectContainerResponse containerInfo;
	final private Container container;

	public FiddleContainer(InspectContainerResponse containerInfo) {
		this.containerInfo = containerInfo;
		DockerClient dockerClient = FiddleUi.getDockerservice().getDockerClient();
		List<Container> containerList = dockerClient.listContainersCmd().withStatusFilter("running").exec();
		Container c = null;
		for (Container container : containerList) {
			if (getId().equals(container.getId())) {
				c = container;
				break;
			}
		}
		this.container = c;
	}

	public String getId() {
		return containerInfo.getId();
	}

	public String getFiddleAppPath() {
		String path = null;
		for (Mount mount : getContainerInfo().getMounts()) {
			String target = mount.getDestination().getPath();
			if ("/webapp/fiddleapp".equals(target)) {
				path = mount.getSource();
			}

		}
		return path;
	}

	public int getFiddlePort() {
		ContainerPort[] ports = getContainer().getPorts();
		for (ContainerPort p : ports) {
			if (p.getPrivatePort() == 8080) {
				return p.getPublicPort();
			}
		}
		return -1;
	}

	public Container getContainer() {
		return container;
	}

	public InspectContainerResponse getContainerInfo() {
		return containerInfo;
	}

	public String getName() {
		return containerInfo.getName();
	}

	public boolean isRunning() {
		return containerInfo.getState().getRunning();
	}

	public boolean isCreated() {
		String status = containerInfo.getState().getStatus();
		return status.equals("created");
	}

	public static int getFiddlePort(Container container) {
		ContainerPort[] ports = container.getPorts();
		for (ContainerPort p : ports) {
			if (p.getPrivatePort() == 8080) {
				return p.getPublicPort();
			}
		}
		return -1;
	}

	public File getFiddleDirectory() {
		return new File(getFiddleAppPath());
	}

}
