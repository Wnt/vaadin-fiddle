package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.component.FileEditor;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.WindowOutput;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.util.FilesystemContainer;
import com.vaadin.v7.ui.Tree;

public class ContainerView extends CustomComponent implements View {

	private TabSheet editorTabs;
	private FiddleContainer fiddleContainer;
	private Window fiddleWindow;
	private String dockerId;
	private Map<File, FileEditor> fileToEditorMap = new HashMap<>();
	private Tree tree;

	@SuppressWarnings("deprecation")
	@Override
	public void enter(ViewChangeEvent event) {
		setSizeFull();

		dockerId = event.getParameters();

		if (!FiddleSession.getCurrent().ownsContainer(dockerId)) {
			UI.getCurrent().getNavigator().navigateTo("fork/" + dockerId);
			return;
		}

		HorizontalSplitPanel editorSplit = new HorizontalSplitPanel();
		Layout toolbar = new CssLayout();
		toolbar.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
		VerticalLayout rootLayout = new VerticalLayout(toolbar, editorSplit);
		setCompositionRoot(rootLayout);
		rootLayout.setExpandRatio(editorSplit, 1);
		rootLayout.setSizeFull();

		editorSplit.setSizeFull();

		editorSplit.setSplitPosition(250, Unit.PIXELS);

		Button saveButton = new Button("Save", FontAwesome.SAVE);
		saveButton.setDescription("Save all open files");
		saveButton.addClickListener(e -> {
			saveAllFiles();
			restartJetty();
		});
		toolbar.addComponent(saveButton);

		Button forkButton = new Button("Fork", FontAwesome.COPY);
		forkButton.setDescription("Create a fork of the currently saved state");

		BrowserWindowOpener opener = new BrowserWindowOpener("#!fork/" + dockerId);
		opener.extend(forkButton);

		toolbar.addComponent(forkButton);

		readContainerInfo();
		Page.getCurrent().setTitle(fiddleContainer.getName() + " - Container - VaadinFiddle");

		FiddleUi.getDockerservice().setOwner(dockerId, UI.getCurrent());

		File fiddleDirectory = new File(fiddleContainer.getFiddleAppPath());

		FilesystemContainer f = new FilesystemContainer(fiddleDirectory);
		tree = new Tree("", f);
		editorSplit.setFirstComponent(tree);
		tree.setSizeFull();
		editorTabs = new TabSheet();

		// TODO use closehandler that checks for unsaved modifications
		editorTabs.setCloseHandler((tabsheet, tabContent) -> {
			FileEditor e = (FileEditor) tabContent;
			fileToEditorMap.remove(e.getFile());
			editorTabs.removeComponent(e);
		});
		editorSplit.setSecondComponent(editorTabs);
		editorTabs.setSizeFull();

		Collection<String> containerPropertyIds = f.getContainerPropertyIds();

		tree.setItemCaptionPropertyId("Name");

		tree.addValueChangeListener(e -> {
			File selectedFile = (File) tree.getValue();
			if (selectedFile == null) {
				return;
			}
			if (fileToEditorMap.get(selectedFile) != null) {
				editorTabs.setSelectedTab(fileToEditorMap.get(selectedFile));
				return;
			}
			FileEditor fileEditor = new FileEditor(selectedFile);
			fileEditor.setSizeFull();

			fileToEditorMap.put(selectedFile, fileEditor);

			String fileName = selectedFile.getName();
			Tab tab = editorTabs.addTab(fileEditor, fileName);
			fileEditor.addTextChangeListener(te -> {
				if (!te.getText().equals(fileEditor.getPropertyDataSource().getValue())) {
					tab.setCaption(fileName + " *");
				} else {
					tab.setCaption(fileName);
				}

			});
			tab.setClosable(true);

			editorTabs.setSelectedTab(tab);
		});

		autoexpandAndSelectFirstJavaFile(fiddleDirectory);

		tree.addExpandListener(e -> {
			f.getItem(e.getItemId());
			Collection<File> children = f.getChildren(e.getItemId());
			if (children.size() == 1) {
				for (File child : children) {
					tree.expandItem(child);
				}
			}

		});

		if (fiddleContainer.isRunning()) {
			createFiddleWindow();
		} else if (fiddleContainer.isCreated()) {

			FiddleUi.getDockerservice().startContainer(dockerId);

			readContainerInfo();

			FiddleUi.getDockerservice().runJetty(dockerId, createOutputWindow());
		} else {
			restartJetty();
		}
	}

	private void autoexpandAndSelectFirstJavaFile(File fiddleDirectory) {
		File javaFile = findFirstJavaFile(fiddleDirectory);
		if (javaFile == null) {
			return;
		}

		ArrayList<File> pathToJava = new ArrayList<>();

		File fs = javaFile;

		while (!fs.getParentFile().equals(fiddleDirectory)) {
			pathToJava.add(fs.getParentFile());
			fs = fs.getParentFile();
		}
		for (int i = pathToJava.size() - 1; i >= 0; i--) {
			tree.expandItem(pathToJava.get(i));
		}

		tree.setValue(javaFile);
	}

	private File findFirstJavaFile(File directory) {

		FileFilter fileFilter = new WildcardFileFilter("*.java");
		File[] files = directory.listFiles(fileFilter);

		if (files.length > 0) {
			return files[0];
		} else {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					File subJava = findFirstJavaFile(file);
					if (subJava != null) {
						return subJava;
					}
				}
			}
		}
		return null;

	}

	private void readContainerInfo() {
		fiddleContainer = FiddleUi.getDockerservice().getFiddleContainerById(dockerId);
	}

	private void createFiddleWindow() {
		String host = Page.getCurrent().getLocation().getHost();
		BrowserFrame frame = new BrowserFrame("",
				new ExternalResource("http://" + host + ":" + fiddleContainer.getFiddlePort()));
		frame.setSizeFull();
		fiddleWindow = new Window("Fiddle app", frame);
		fiddleWindow.setWidth("400px");
		fiddleWindow.setHeight("800px");
		fiddleWindow.setPositionX(Page.getCurrent().getBrowserWindowWidth() - 430);
		fiddleWindow.setPositionY(30);

		fiddleWindow.setClosable(false);

		UI.getCurrent().addWindow(fiddleWindow);
	}

	private void restartJetty() {
		Collection<Window> windows = new ArrayList<>(UI.getCurrent().getWindows());
		for (Window w : windows) {
			w.close();
		}
		WindowOutput consoleOutput = createOutputWindow();
		FiddleUi.getDockerservice().restartJetty(fiddleContainer.getId(), consoleOutput, UI.getCurrent());
		readContainerInfo();
	}

	private WindowOutput createOutputWindow() {
		WindowOutput consoleOutput = new WindowOutput();
		consoleOutput.addJettyStartListener(() -> {
			editorTabs.getUI().access(new Runnable() {

				@Override
				public void run() {
					createFiddleWindow();

				}
			});
		});
		return consoleOutput;
	}

	private void saveAllFiles() {
		for (Component component : editorTabs) {
			FileEditor fileEditor = (FileEditor) component;
			fileEditor.commit();
			editorTabs.getTab(fileEditor).setCaption(fileEditor.getFile().getName());
		}
	}

}
