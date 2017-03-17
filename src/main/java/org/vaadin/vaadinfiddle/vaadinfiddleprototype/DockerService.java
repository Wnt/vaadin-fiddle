package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FileUtils;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.model.Bind;
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

	public CreateContainerResponse createFiddleContainer() {
		CreateContainerCmd containerStub = createContainerStub();

		Volume volume = new Volume("/webapp/fiddleapp");
		CreateContainerResponse container = containerStub

				.withVolumes(volume)

				.exec();
		return container;
	}

	private CreateContainerCmd createContainerStub() {
		ExposedPort exposedPort = new ExposedPort(8080);
		PortBinding portBinding = new PortBinding(new Binding(null, null), exposedPort);
		return dockerClient

				.createContainerCmd("vaadin-stub")

				.withCmd("bash")

				.withTty(true)

				.withPortBindings(portBinding)

				.withExposedPorts(exposedPort);
	}

	public String cloneContainer(String id) {
		CreateContainerCmd containerStub = createContainerStub();
		String datadir = getDatadirById(id);

		String datadirClone = "/tmp/fiddle_" + UUID.randomUUID();

		File source = new File(datadir);
		File dest = new File(datadirClone);
		try {
			FileUtils.copyDirectory(source, dest);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Volume volume = new Volume("/webapp/fiddleapp");
		Bind volumeBind = new Bind(datadirClone, volume);

		CreateContainerResponse container = containerStub

				.withBinds(volumeBind)

				.exec();
		return container.getId();
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

		Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
		DockerClient throwAwayDockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();

		ExecCreateCmd cmdBuild = throwAwayDockerClient.execCreateCmd(id).withCmd("su", "vaadin", "-c",
				"cd /webapp/fiddleapp; mvn jetty:run");
		if (stdout != null) {
			cmd = cmdBuild.withAttachStdout(true).exec();

			resultCallback = new ExecStartResultCallback(stdout, System.err);
		} else {
			cmd = cmdBuild.exec();

			resultCallback = new ExecStartResultCallback();
		}
		ExecStartResultCallback start = throwAwayDockerClient

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

	public String getDatadirById(String id) {

		InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();

		for (Mount mount : containerInfo.getMounts()) {
			String target = mount.getDestination().getPath();
			if ("/webapp/fiddleapp".equals(target)) {
				return mount.getSource();
			}

		}
		return null;
	}

	private InspectContainerResponse getContainerInfoById(String id) {
		InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();
		return containerInfo;
	}
}
