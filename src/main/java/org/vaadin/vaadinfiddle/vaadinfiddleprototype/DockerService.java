package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class DockerService {

	public static final int MAX_RUNNING_CONTAINERS = 4;
	final private DockerClient dockerClient;
	private List<String> runningContainers = new CopyOnWriteArrayList<>();

	private Map<String, UI> containerOwnerUis = new HashMap<>();

	public DockerService() {

		Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
		dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
	}

	public DockerClient getDockerClient() {
		return dockerClient;
	}

	CreateContainerResponse createFiddleContainer() {
		ExposedPort exposedPort = new ExposedPort(8080);
		PortBinding portBinding = new PortBinding(new Binding(null, null), exposedPort);
		Volume volume = new Volume("/webapp/fiddleapp");
		CreateContainerResponse container = dockerClient.createContainerCmd("vaadin-stub").withVolumes(volume)
				.withCmd("bash").withTty(true).withPortBindings(portBinding).withExposedPorts(exposedPort).exec();
		return container;
	}

	public void startContainer(String id) {
		ensureCapacity(1);
		dockerClient.startContainerCmd(id).exec();
		runningContainers.add(id);
	}

	private void ensureCapacity(int additionalContainers) {
		while ((runningContainers.size() + additionalContainers) > MAX_RUNNING_CONTAINERS) {
			String leastActiveContainer = runningContainers.get(0);
			InspectContainerResponse containerInfo = getContainerInfoById(leastActiveContainer);
			ContainerState state = containerInfo.getState();
			Boolean running = state.getRunning();
			if (running) {
				dockerClient.killContainerCmd(leastActiveContainer).exec();
			}

			runningContainers.remove(leastActiveContainer);
			UI ownerUi = containerOwnerUis.get(leastActiveContainer);
			if (ownerUi != null && ownerUi.isAttached()) {
				ownerUi.access(() -> {
					Notification.show(
							"'" + containerInfo.getName() + "' was stopped due to inactivity. Save to restart it.",
							Notification.Type.WARNING_MESSAGE);
				});
			}
		}
	}

	public void reactivateContainer(String id) {
		runningContainers.remove(id);
		runningContainers.add(id);
	}

	public void runJetty(String id) {
		runJetty(id, null);
	}

	public void runJetty(String id, OutputStream stdout) {
		ExecCreateCmdResponse cmd;
		ExecStartResultCallback resultCallback;

		ExecCreateCmd cmdBuild = dockerClient.execCreateCmd(id).withCmd("su", "vaadin", "-c",
				"cd /webapp/fiddleapp; mvn jetty:run");
		if (stdout != null) {
			cmd = cmdBuild.withAttachStdout(true).exec();

			resultCallback = new ExecStartResultCallback(new BufferedOutputStream(stdout), System.err);
		} else {
			cmd = cmdBuild.exec();

			resultCallback = new ExecStartResultCallback();
		}

		ExecStartResultCallback start = dockerClient

				.execStartCmd(cmd.getId())

				.withDetach(false)

				.withTty(true).exec(resultCallback);

	}

	public void restartJetty(String id, OutputStream os, UI owner) {

		InspectContainerResponse containerInfo = getContainerInfoById(id);
		ContainerState state = containerInfo.getState();
		Boolean running = state.getRunning();
		ensureCapacity(running ? 0 : 1);
		dockerClient.restartContainerCmd(id).exec();
		reactivateContainer(id);
		runJetty(id, os);
		setOwner(id, owner);
	}

	public void setOwner(String id, UI owner) {
		containerOwnerUis.put(id, owner);
	}

	public FiddleContainer getFiddleContainerById(String id) {
		InspectContainerResponse containerInfo = getContainerInfoById(id);
		return new FiddleContainer(containerInfo);
	}

	private InspectContainerResponse getContainerInfoById(String id) {
		InspectContainerResponse containerInfo = getDockerClient().inspectContainerCmd(id).exec();
		return containerInfo;
	}
}
