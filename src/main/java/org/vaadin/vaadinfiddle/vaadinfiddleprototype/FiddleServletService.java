package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;

public class FiddleServletService extends VaadinServletService {
	

	public FiddleServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration)
			throws ServiceException {
		super(servlet, deploymentConfiguration);
	}
	
	@Override
	protected VaadinSession createVaadinSession(VaadinRequest request) throws ServiceException {
        return new FiddleSession(this);
	}
}
