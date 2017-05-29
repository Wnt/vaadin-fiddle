package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

public class CreatorView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {
		CreateContainerResponse container = FiddleUi.getDockerservice().createFiddleContainer();
		String id = container.getId();
		FiddleSession.getCurrent().addOwnedContainer(id);

		Notification notification = new Notification("Creating fiddle",
				"Booting up your fresh fiddle. This shouldn't longer than a few seconds!", Type.TRAY_NOTIFICATION);
		notification.setDelayMsec(5000);
		notification.show(Page.getCurrent());
		
		UI.getCurrent().getNavigator().navigateTo("container/" + id);
	}

}
