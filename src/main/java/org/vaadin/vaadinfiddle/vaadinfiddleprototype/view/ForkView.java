package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import org.apache.commons.lang.StringUtils;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi.ViewIds;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

public class ForkView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {

		String id = event.getParameters();

		String cloneId = FiddleUi.getDockerservice().cloneContainer(id);
		if (cloneId == null) {

			Notification notification = new Notification("Error: unknown container",
					"You tried to navigate to an unknown container '" + StringUtils.abbreviate(id, 12) + "'",
					Type.ERROR_MESSAGE);

			notification.setDelayMsec(-1);
			notification.show(Page.getCurrent());
			UI.getCurrent().getNavigator().navigateTo(ViewIds.CREATOR + "");
			return;
		}
		FiddleSession.getCurrent().addOwnedContainer(cloneId);

		Notification notification = new Notification("Forking fiddle",
				"Booting up the fiddle fork. This shouldn't longer than a few seconds!", Type.TRAY_NOTIFICATION);
		notification.setDelayMsec(5000);
		notification.show(Page.getCurrent());

		UI.getCurrent().getNavigator().navigateTo(ViewIds.CONTAINER + "/" + cloneId);

	}

}
