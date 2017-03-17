package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.UI;

public class ForkView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {
		
		String id = event.getParameters();
		
		String cloneId = FiddleUi.getDockerservice().cloneContainer(id);
		FiddleSession.getCurrent().addOwnedContainer(cloneId);
		
		UI.getCurrent().getNavigator().navigateTo("container/"+cloneId);

	}

}
