package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;

public class ContainerView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {

		String p = event.getParameters();
		setCompositionRoot(new Label("You have reached the container page of container ID " + p));
		InspectContainerResponse containerInfo = FiddleUi.getDockerservice().getDockerClient().inspectContainerCmd(p)
				.exec();

		Page.getCurrent().setTitle(containerInfo.getName() + " - Container - VaadinFiddle");
	}

}
