package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;

public class FiddleSession extends VaadinSession {
	private static final String OWNED_CONTAINERS = "owned_containers";

	public FiddleSession(VaadinService service) {
		super(service);
	}
	
	public boolean ownsContainer(String id) {
		return true;
	}
	
	public void addOwnedContainer(String id) {
		getOwnedContainers().add(id);
	}

	public List<String> getOwnedContainers() {
		List<String>  ownedContainers = (List<String>) this.getSession().getAttribute(OWNED_CONTAINERS);
		if (ownedContainers == null ) {
			ownedContainers = new ArrayList<>();
			this.getSession().setAttribute(OWNED_CONTAINERS, ownedContainers);
		}
		return ownedContainers;
	}
	

    public static FiddleSession getCurrent() {
        return (FiddleSession) VaadinSession.getCurrent();
    }
	
}