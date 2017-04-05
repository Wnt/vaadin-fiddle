package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Notification.Type;

public class ForkView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {
		
		String id = event.getParameters();
		
		String cloneId = FiddleUi.getDockerservice().cloneContainer(id);
		FiddleSession.getCurrent().addOwnedContainer(cloneId);

		Notification notification = new Notification("Forking fiddle",
				"Booting up the fiddle fork. This shouldn't longer than a few seconds!", Type.TRAY_NOTIFICATION);
		notification.setDelayMsec(5000);
		notification.show(Page.getCurrent());
		
		UI.getCurrent().getNavigator().navigateTo("container/"+cloneId);

	}

}
