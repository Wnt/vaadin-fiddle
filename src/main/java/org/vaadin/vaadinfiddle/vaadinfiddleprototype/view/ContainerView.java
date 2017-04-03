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
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
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
		toolbar.addStyleName("tools");

		Image logo = new Image(null, new ThemeResource("VaadinFiddle.png"));
		logo.setWidth("252px");
		logo.setHeight("66px");
		Label forkLabel = new Label(
				"<a href=\"https://github.com/Wnt/vaadin-fiddle\" class=\"github-corner\" aria-label=\"View source on Github\"><svg width=\"80\" height=\"80\" viewBox=\"0 0 250 250\" style=\"fill:#151513; color:#fff; position: absolute; top: 0; border: 0; right: 0;\" aria-hidden=\"true\"><path d=\"M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z\"></path><path d=\"M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2\" fill=\"currentColor\" style=\"transform-origin: 130px 106px;\" class=\"octo-arm\"></path><path d=\"M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z\" fill=\"currentColor\" class=\"octo-body\"></path></svg></a><style>.github-corner:hover .octo-arm{animation:octocat-wave 560ms ease-in-out}@keyframes octocat-wave{0%,100%{transform:rotate(0)}20%,60%{transform:rotate(-25deg)}40%,80%{transform:rotate(10deg)}}@media (max-width:500px){.github-corner:hover .octo-arm{animation:none}.github-corner .octo-arm{animation:octocat-wave 560ms ease-in-out}}</style>",
				ContentMode.HTML);

		HorizontalLayout topBar = new HorizontalLayout(toolbar, logo, forkLabel);
		topBar.setComponentAlignment(logo, Alignment.TOP_CENTER);
		topBar.setComponentAlignment(forkLabel, Alignment.TOP_RIGHT);
		topBar.setWidth("100%");

		VerticalLayout rootLayout = new VerticalLayout(topBar, editorSplit);
		rootLayout.setMargin(false);
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
				new ExternalResource("http://" + host + "/container/" + fiddleContainer.getId()));
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
