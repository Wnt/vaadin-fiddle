package org.vaadin.vaadinfiddle.vaadinfiddleprototype.data;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;

public class FiddleContainer {

	final private InspectContainerResponse containerInfo;

	public FiddleContainer(InspectContainerResponse containerInfo) {
		this.containerInfo = containerInfo;
	}
	
	public String getFiddleAppPath() {
		String path = null;
		for (Mount mount : containerInfo.getMounts()) {
			String target = mount.getDestination().getPath();
			if ("/webapp/fiddleapp".equals(target)) {
				path= mount.getSource();
			}

		}
		return path;
	}
}
