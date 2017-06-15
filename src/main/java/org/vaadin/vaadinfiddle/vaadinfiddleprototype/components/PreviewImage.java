package org.vaadin.vaadinfiddle.vaadinfiddleprototype.components;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class PreviewImage extends CustomComponent {
	public PreviewImage(String frameURL, String imageName) {
		String imgFilename = imageName.replace('/', '_');
		ProgressBar progressBar = new ProgressBar();
		progressBar.setIndeterminate(true);
		Label waitLabel = new Label("Generating preview image");
		VerticalLayout compositionRoot = new VerticalLayout(waitLabel, progressBar);
		compositionRoot.setComponentAlignment(waitLabel, Alignment.BOTTOM_CENTER);
		compositionRoot.setComponentAlignment(progressBar, Alignment.TOP_CENTER);
		compositionRoot.setSizeFull();
		setCompositionRoot(compositionRoot);

		File baseDirectory = UI.getCurrent().getSession().getService().getBaseDirectory();
		String targetDirPath = baseDirectory.getAbsolutePath() + "/VAADIN/img";
		File targetDir = new File(targetDirPath);
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		Thread imgGeneratorThread = new Thread(() -> {
			ProcessBuilder builder = new ProcessBuilder("phantomjs", "src/main/resources/screenshot.js", frameURL,
					targetDirPath + "/" + imgFilename);
			boolean success = false;
			try {
				Process process = builder.start();
				success = process.waitFor(30, TimeUnit.SECONDS);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			if (success) {
				getUI().access(() -> {
					setCompositionRoot(new Image(null, new ExternalResource("vaadin://img/" + imgFilename)));
				});
			} else {
				getUI().access(() -> {
					setCompositionRoot(new Label(
							"Failed to generate preview image. Please make sure the initial UI loads within 30 seconds and try again."));
				});
			}
		});

		imgGeneratorThread.start();

	}
}