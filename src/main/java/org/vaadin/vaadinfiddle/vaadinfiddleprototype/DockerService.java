package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.Ulimit;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

public class DockerService {

	private static final int CPU_SHARES = 4000000;
	private static final int CPU_PERIOD = 1000000;
	private static final String IMAGE_NAME = "vaadin-stub";
	private static final long MEMORY_LIMIT = 1024l * 1024l * 384l;
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
		CreateContainerCmd command = containerStub

				.withVolumes(volume);

		setLimits(command);

		CreateContainerResponse container = command.exec();
		return container;
	}

	private void setLimits(CreateContainerCmd command) {
		command.withUlimits(getUlimits())

		.withMemory(MEMORY_LIMIT)

		.withCpuPeriod(CPU_PERIOD)

		.withCpuShares(CPU_SHARES);
	}

	private Ulimit getUlimits() {
		return new Ulimit("nproc", 1024, 1024);
	}

	private CreateContainerCmd createContainerStub() {
		ExposedPort exposedPort = new ExposedPort(8080);
		PortBinding portBinding = new PortBinding(new Binding(null, null), exposedPort);
		CreateContainerCmd cmd = dockerClient

				.createContainerCmd(IMAGE_NAME)

				.withCmd("bash")

				.withTty(true)

				.withPortBindings(portBinding).withExposedPorts(exposedPort);
		setLimits(cmd);
		
		return cmd;
	}

	public String cloneContainer(String id) {
		CreateContainerCmd containerStub = createContainerStub();
		String datadir = getDatadirById(id);
		if (datadir == null) {
			return null;
		}

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

		configureProxy(id);

	}

	private void configureProxy(String id) {
		Container container = null;

		List<Container> containerList = dockerClient.listContainersCmd().withStatusFilter("running").exec();
		for (Container c : containerList) {
			if (id.equals(c.getId())) {
				container = c;
				break;
			}
		}

		int port = FiddleContainer.getFiddlePort(container);

		String proxyConfigPath = "/etc/nginx/fiddle-config/container-conf.d/" + id + ".conf";
		Path file = Paths.get(proxyConfigPath);
		try {
			List<String> lines = Arrays.asList(

					"location /container/" + id + "/PUSH {",

					"  proxy_pass http://localhost:" + port + "/PUSH;",

					"  proxy_http_version 1.1;",

					"  proxy_set_header Upgrade $http_upgrade;",

					"  proxy_set_header Connection \"upgrade\";",

					"  proxy_read_timeout 15m;",

					"}",

					"location /container/" + id + "/ {",

					"  proxy_pass http://127.0.0.1:" + port + "/;",

					"  proxy_redirect default;",

					"  proxy_cookie_path / /container/" + id + "/;",

					"  proxy_read_timeout 15m;",

					"  add_header X-Frame-Options SAMEORIGIN;",

					"}"

			);
			Files.write(file, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// add following line to /etc/sudoers:
		// %docker ALL=NOPASSWD: /bin/systemctl reload nginx.service

		Runtime rt = Runtime.getRuntime();
		try {
			Process pr = rt.exec("sudo systemctl reload nginx.service");
			int retVal = pr.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

					Notification notification = new Notification("Fiddle stopped",
							"This fiddle was stopped as someone else needed the server resources for their fiddle. Just hit save to restart and reclaim the resources!",
							Type.TRAY_NOTIFICATION);
					notification.setDelayMsec(Notification.DELAY_FOREVER);
					notification.show(Page.getCurrent());
				});
			}
		}
	}

	public void reactivateContainer(String id) {
		runningContainers.remove(id);
		runningContainers.add(id);
	}

	public void runJetty(String id, OutputStream stdout, UI owner) {

		setOwner(id, owner);
		ExecCreateCmdResponse cmd;
		ExecStartResultCallback resultCallback;

		Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
		DockerClient throwAwayDockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();

		ExecCreateCmd cmdBuild = throwAwayDockerClient.execCreateCmd(id).withCmd("su", "vaadin", "-c",
				"cd /webapp/fiddleapp; mvn clean jetty:run");
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
		runJetty(id, os, owner);

		configureProxy(id);
	}

	public void setOwner(String id, UI owner) {
		containerOwnerUis.put(id, owner);
	}

	public FiddleContainer getFiddleContainerById(String id) {
		InspectContainerResponse containerInfo = getContainerInfoById(id);
		return new FiddleContainer(containerInfo);
	}

	public String getDatadirById(String id) {

		try {
			InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();

			for (Mount mount : containerInfo.getMounts()) {
				String target = mount.getDestination().getPath();
				if ("/webapp/fiddleapp".equals(target)) {
					return mount.getSource();
				}

			}
		} catch (NotFoundException e) {
			System.err.println("tried to get datadir of nonexisting container: " + id);
		}
		return null;
	}

	private InspectContainerResponse getContainerInfoById(String id) {
		InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(id).exec();
		return containerInfo;
	}

	public void unregisterUI(FiddleUi fiddleUi) {
		ArrayList<String> toUnregister = new ArrayList<>();
		for (Entry<String, UI> entry : containerOwnerUis.entrySet()) {
			if (entry.getValue() == fiddleUi) {
				toUnregister.add(entry.getKey());
			}
		}
		for (String string : toUnregister) {
			containerOwnerUis.remove(string);
		}
	}

	public List<Container> getContainers() {
		ListContainersCmd listCmd = dockerClient.listContainersCmd();
		List<Container> containerList = listCmd.withShowAll(true).exec();
		return containerList;
	}
}
