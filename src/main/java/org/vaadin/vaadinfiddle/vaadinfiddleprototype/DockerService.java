package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;

public class DockerService {

	final private DockerClient dockerClient;

	public DockerService() {

		Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
		dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
	}

	public DockerClient getDockerClient() {
		return dockerClient;
	}
}
