package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebListener;

@WebListener
public class CookieNamer implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		String cookieName = "FIDDLE_SESSION";

		SessionCookieConfig scf = sce.getServletContext().getSessionCookieConfig();

		scf.setName(cookieName);
	}

}
