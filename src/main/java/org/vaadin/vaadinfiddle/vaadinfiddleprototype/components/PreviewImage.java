package org.vaadin.vaadinfiddle.vaadinfiddleprototype.components;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.PreviewImageServlet;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class PreviewImage extends CustomComponent {
	public PreviewImage(String frameURL, String imageName) {
		ProgressBar progressBar = new ProgressBar();
		progressBar.setIndeterminate(true);
		Label waitLabel = new Label("Generating preview image");
		VerticalLayout compositionRoot = new VerticalLayout(waitLabel, progressBar);
		compositionRoot.setComponentAlignment(waitLabel, Alignment.BOTTOM_CENTER);
		compositionRoot.setComponentAlignment(progressBar, Alignment.TOP_CENTER);
		compositionRoot.setSizeFull();
		setCompositionRoot(compositionRoot);

		ServletContext servletContext = VaadinServlet.getCurrent().getServletContext();
		File baseDirectory = VaadinService.getCurrent().getBaseDirectory();

		String targetDirPath = PreviewImageServlet.getResourcePath(servletContext, "/preview-image");

		File targetDir = new File(targetDirPath);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		String imageAbsolutePath = targetDirPath + "/" + imageName;
//		File imageFile = new File(imageAbsolutePath);

//		if (imageFile.exists()) {
//			showImage(imageName, servletContext);
//		} else {
			// TODO replace with a thread pool
			Thread imgGeneratorThread = new Thread(() -> {
				boolean success = createImage(frameURL, baseDirectory + "", imageAbsolutePath);
				if (!isAttached()) {
					return;
				}
				if (success) {
					getUI().access(() -> {
						showImage(imageName, servletContext);
					});
				} else {
					getUI().access(() -> {
						setCompositionRoot(new Label(
								"Failed to generate preview image. Please make sure the initial UI loads within 30 seconds and try again."));
					});
				}
			});

			imgGeneratorThread.start();
//		}

	}

	public static boolean createImage(String frameURL, String baseDirectory, String imageAbsolutePath) {
		// TODO auto-detect paths from environment
		// deployment time paths
		String phantomJsExecutable = "/opt/phantomjs-2.1.1-linux-x86_64/bin/phantomjs";
		String pathToScreenshotScript = baseDirectory + "/WEB-INF/classes/screenshot.js";
		// Development time paths
//		String phantomJsExecutable = "phantomjs";
//		String pathToScreenshotScript = "src/main/resources/screenshot.js";
		ProcessBuilder builder = new ProcessBuilder(phantomJsExecutable, pathToScreenshotScript, frameURL,
				imageAbsolutePath);

		boolean success = false;
		try {
			Process process = builder.start();
			success = process.waitFor(30, TimeUnit.SECONDS);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return success;
	}

	private void showImage(String imageName, ServletContext servletContext) {
		Image img = new Image(null,
				new ExternalResource(UI.getCurrent().getUiRootPath() + "/preview-image/" + imageName));
		img.setWidth("800px");
		img.setHeight("355px");
		setCompositionRoot(img);
	}
}