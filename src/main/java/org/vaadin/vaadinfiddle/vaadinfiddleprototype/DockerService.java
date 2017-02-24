package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

public class DockerService {

	final private DockerClient dockerClient;

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
		dockerClient.startContainerCmd(id).exec();
	}

	public void runJetty(String id, OutputStream stdout) {
		ExecCreateCmdResponse cmd = dockerClient.execCreateCmd(id)
				.withCmd("su", "vaadin", "-c", "cd /webapp/fiddleapp; mvn jetty:run").withAttachStdout(true).exec();

		ExecStartResultCallback start = dockerClient.execStartCmd(cmd.getId()).withDetach(false).withTty(true)
				.exec(new ExecStartResultCallback(new BufferedOutputStream(stdout), System.err));
	}

	public void restartJetty(String id, OutputStream os) {
		dockerClient.restartContainerCmd(id).exec();
		runJetty(id, os);
	}
}
