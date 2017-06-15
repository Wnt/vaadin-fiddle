package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import java.io.File;
import java.net.URI;

import org.vaadin.addon.codemirror.CodeMirrorField;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileToStringValueProvider;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileTypeUtil;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.StringToFileSetter;

import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;

public class PreviewView extends CustomComponent implements View {
	private String fileToSelect;
	private String dockerId;
	private FiddleContainer fiddleContainer;
	private File selectedFile;

	@Override
	public void enter(ViewChangeEvent event) {
		parseParameters(event.getParameters());
		readContainerInfo();

		File fiddleDirectory = fiddleContainer.getFiddleDirectory();
		
		selectedFile = new File(fiddleDirectory.getAbsolutePath() + fileToSelect);
		
		if (!selectedFile.getAbsolutePath().startsWith(fiddleDirectory.getAbsolutePath())) {
			Notification.show("Error loading file", "File is outside of fiddle app", Type.ERROR_MESSAGE);
			return;
		}
		createLayout();
		updateTitle();
	}

	private void updateTitle() {
		String file = selectedFile != null ? selectedFile.getName() + " - " : "";
		Page.getCurrent().setTitle(file + fiddleContainer.getName() + " - Preview - VaadinFiddle");
	}
	private void createLayout() {
		CodeMirrorField codeMirrorField = new CodeMirrorField();
		codeMirrorField.setLineNumbers(false);
		codeMirrorField.removeGutter("CodeMirror-linenumbers");
		codeMirrorField.removeGutter("CodeMirror-foldgutter");

		codeMirrorField.setSizeFull();
		
		String fileName = selectedFile.getName();
		codeMirrorField.setMode(FileTypeUtil.getMimeTypeByFileExtension(fileName));

		Binder<File> binder = new Binder<File>();
		BindingBuilder<File, String> bb = binder.forField(codeMirrorField);

		bb.bind(new FileToStringValueProvider(), (file, value) -> {});
		binder.readBean(selectedFile);

		URI location = Page.getCurrent().getLocation();
		String host = location.getHost();
		String scheme = location.getScheme();
		BrowserFrame frame = new BrowserFrame(null,
				new ExternalResource(scheme + "://" + host + "/container/" + fiddleContainer.getId()));
		frame.setSizeFull();
		frame.setId("result-frame");

		HorizontalLayout rootHorizontalSplit = new HorizontalLayout(codeMirrorField, frame);
		rootHorizontalSplit.setSizeFull();
		CssLayout rootLaytout = new CssLayout(rootHorizontalSplit);
		rootLaytout.setSizeFull();
		setCompositionRoot(rootLaytout);
		setSizeFull();
		addStyleName("preview-view");
		addStyleName("scale-down");
		setId("preview-view");
	}

	private void readContainerInfo() {
		fiddleContainer = FiddleUi.getDockerservice().getFiddleContainerById(dockerId);
	}

	/**
	 * Parses view enter parameters into fileToSelect and dockerId fields
	 * 
	 * @param params
	 */
	private void parseParameters(String params) {
		int idFileSplit = params.indexOf("/");
		fileToSelect = null;
		if (idFileSplit != -1) {
			dockerId = params.substring(0, idFileSplit);
			fileToSelect = params.substring(idFileSplit);
		} else {
			dockerId = params;
		}
	}
}
