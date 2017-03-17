package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.UI;

public class CreatorView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {
		CreateContainerResponse container = FiddleUi.getDockerservice().createFiddleContainer();
		String id = container.getId();

		UI.getCurrent().getNavigator().navigateTo("container/" + id);
	}

}
