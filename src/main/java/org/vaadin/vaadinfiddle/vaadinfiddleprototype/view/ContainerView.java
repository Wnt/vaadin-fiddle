package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.vaadin.addon.codemirror.CodeMirrorField;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi.ViewIds;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.components.TreeWithContextMenu;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileSystemProvider;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileToStringValueProvider;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileTypeUtil;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.PanelOutput;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.StringToFileSetter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.contextmenu.GridContextMenu.GridContextMenuOpenListener.GridContextMenuOpenEvent;
import com.vaadin.contextmenu.MenuItem;
import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.Binder.BindingBuilder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.icons.VaadinIcons;
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
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ContainerView extends CustomComponent implements View {

	private TabSheet editorTabs;
	private FiddleContainer fiddleContainer;
	private String dockerId;
	private BiMap<File, Component> fileToEditorMap = HashBiMap.create();
	private VerticalSplitPanel editorTabsAndConsole;
	private HorizontalSplitPanel mainAreaAndFiddleResult;
	private boolean startedMessageShown = false;
	private TreeWithContextMenu<File> tree;
	private Map<File, Binder<File>> fileToBinderMap = new HashMap<>();
	private List<File> expandedDirectories = new ArrayList<>();
	private File selectedFile;

	@Override
	public void enter(ViewChangeEvent event) {
		setSizeFull();

		addStyleName("container-view");

		String params = event.getParameters();
		int idFileSplit = params.indexOf("/");
		String fileToSelect = null;
		if (idFileSplit != -1) {
			dockerId = params.substring(0, idFileSplit);
			fileToSelect = params.substring(idFileSplit);
		} else {
			dockerId = params;
		}

		if (!FiddleSession.getCurrent().ownsContainer(dockerId)) {
			UI.getCurrent().getNavigator().navigateTo(ViewIds.FORK + "/" + dockerId);
			return;
		}

		HorizontalSplitPanel editorSplit = new HorizontalSplitPanel();
		Layout actionsForCurrentFiddle = new CssLayout();
		actionsForCurrentFiddle.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);

		Image logo = new Image(null, new ThemeResource("VaadinFiddle.png"));
		logo.setWidth("252px");
		logo.setHeight("66px");
		Label forkLabel = new Label(
				"<a href=\"https://github.com/Wnt/vaadin-fiddle\" class=\"github-corner\" aria-label=\"View source on Github\"><svg width=\"80\" height=\"80\" viewBox=\"0 0 250 250\" style=\"fill:#151513; color:#fff; position: absolute; top: 0; border: 0; right: 0;\" aria-hidden=\"true\"><path d=\"M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z\"></path><path d=\"M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2\" fill=\"currentColor\" style=\"transform-origin: 130px 106px;\" class=\"octo-arm\"></path><path d=\"M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z\" fill=\"currentColor\" class=\"octo-body\"></path></svg></a><style>.github-corner:hover .octo-arm{animation:octocat-wave 560ms ease-in-out}@keyframes octocat-wave{0%,100%{transform:rotate(0)}20%,60%{transform:rotate(-25deg)}40%,80%{transform:rotate(10deg)}}@media (max-width:500px){.github-corner:hover .octo-arm{animation:none}.github-corner .octo-arm{animation:octocat-wave 560ms ease-in-out}}</style>",
				ContentMode.HTML);

		CssLayout toolbar = new CssLayout(actionsForCurrentFiddle);
		toolbar.addStyleName("tools");
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
		saveButton.setDescription("Save all open files (Ctrl + Enter)");
		saveButton.addClickListener(e -> {
			saveAllFiles();
			restartJetty();

			Notification notification = new Notification("Saving and restarting",
					"This shouldn't take longer than a few seconds", Type.TRAY_NOTIFICATION);
			notification.setDelayMsec(1000);
			notification.show(Page.getCurrent());
		});
		actionsForCurrentFiddle.addComponent(saveButton);
		saveButton.setClickShortcut(KeyCode.ENTER, ModifierKey.CTRL);

		Button forkButton = new Button("Fork", VaadinIcons.COPY_O);
		forkButton.setDescription("Create a copy of the currently saved state");

		BrowserWindowOpener opener = new BrowserWindowOpener("#!fork/" + dockerId);
		opener.extend(forkButton);

		actionsForCurrentFiddle.addComponent(forkButton);

		Button newButton = new Button("New", VaadinIcons.FILE_ADD);
		newButton.setDescription("Create a new fiddle (Alt + N)");
		newButton.addClickListener(e -> {
			getUI().getNavigator().navigateTo("");
		});
		toolbar.addComponent(newButton);
		newButton.setClickShortcut(KeyCode.N, ModifierKey.ALT);

		readContainerInfo();
		updateTitle();

		FiddleUi.getDockerservice().setOwner(dockerId, UI.getCurrent());

		File fiddleDirectory = getFiddleDirectory();

		tree = new TreeWithContextMenu();
		tree.setStyleName("file-picker");
		FileSystemProvider fp = new FileSystemProvider(fiddleDirectory);
		tree.setDataProvider(fp);
		tree.setItemCaptionGenerator(File::getName);

		tree.addExpandListener(expandEvent -> {
			expandedDirectories.add(expandEvent.getExpandedItem());
		});
		tree.addCollapseListener(collapseEvent -> {
			expandedDirectories.remove(collapseEvent.getCollapsedItem());
		});

		tree.setItemIconGenerator(file -> {
			if (file.isDirectory()) {
				if (expandedDirectories.contains(file)) {
					return VaadinIcons.FOLDER_OPEN_O;
				}
				return VaadinIcons.FOLDER_O;
			}
			String name = file.getName().toLowerCase();
			if (name.endsWith(".java") || name.endsWith(".scss") || name.endsWith(".css") || name.endsWith(".xml")) {
				return VaadinIcons.FILE_CODE;
			}
			return VaadinIcons.FILE_O;
		});

		createTreeContextMenu();

		editorSplit.setFirstComponent(tree);
		tree.setSizeFull();
		editorTabs = new TabSheet();
		editorTabsAndConsole = new VerticalSplitPanel(editorTabs, null);
		editorTabsAndConsole.setSizeFull();
		mainAreaAndFiddleResult = new HorizontalSplitPanel(editorTabsAndConsole, null);
		mainAreaAndFiddleResult.setSplitPosition(100, Unit.PERCENTAGE);
		editorSplit.setSecondComponent(mainAreaAndFiddleResult);
		editorTabsAndConsole.setSizeFull();
		editorTabsAndConsole.setSplitPosition(100, Unit.PERCENTAGE);
		editorTabsAndConsole.addStyleName("editor-tabs-and-console");

		// TODO use closehandler that checks for unsaved modifications
		editorTabs.setCloseHandler((tabsheet, tabContent) -> {
			File file = fileToEditorMap.inverse().get(tabContent);
			fileToEditorMap.remove(file);
			fileToBinderMap.remove(file);
			editorTabs.removeComponent(tabContent);
		});
		editorTabs.setSizeFull();

		editorTabs.addSelectedTabChangeListener(selectionEvent -> {
			File selectedFile = fileToEditorMap.inverse().get(editorTabs.getSelectedTab());

			String relativeFilePath = selectedFile.getAbsolutePath()
					.substring(fiddleContainer.getFiddleAppPath().length() + 1);

			String uriFragment = "!" + ViewIds.CONTAINER + "/" + dockerId + "/" + relativeFilePath;
			Page.getCurrent().setUriFragment(uriFragment, false);
			this.selectedFile = selectedFile;
			expandAndSelectFile(relativeFilePath);
			updateTitle();
		});

		tree.addSelectionListener(e -> {
			File selectedFile = tree.asSingleSelect().getValue();
			if (selectedFile == null || selectedFile.isDirectory()) {
				return;
			}
			showFileInEditor(selectedFile);
		});

		if (fileToSelect != null && !fileToSelect.isEmpty()) {
			expandAndSelectFile(fileToSelect);
		} else {
			expandAndSelectFirstJavaFile();
		}

		tree.addExpandListener(e -> {
			File[] children = e.getExpandedItem().listFiles();
			if (children.length == 1) {
				for (File child : children) {
					tree.expand(child);
				}
			}
		});

		Notification startupNotification;
		if (fiddleContainer.isRunning()) {
			createResultFrame();
			startupNotification = new Notification("Console hidden",
					"This fiddle was already running when you got here. If you want to see the console messages just hit save!",
					Type.TRAY_NOTIFICATION);
			startupNotification.setDelayMsec(5000);
			startupNotification.show(Page.getCurrent());
		} else if (fiddleContainer.isCreated()) {

			FiddleUi.getDockerservice().startContainer(dockerId);

			readContainerInfo();

			FiddleUi.getDockerservice().runJetty(dockerId, createConsolePanel());

		} else {
			startupNotification = new Notification("Waking up",
					"This fiddle was sleeping when you got here. Starting it up shouldn't longer than few seconds!",
					Type.TRAY_NOTIFICATION);
			startupNotification.setDelayMsec(5000);
			startupNotification.show(Page.getCurrent());
			restartJetty();
		}
	}

	private void createTreeContextMenu() {
		GridContextMenu<File> contextMenu = tree.getContextMenu();
		contextMenu.addGridBodyContextMenuListener(this::updateTreeMenu);
	}

	private void updateTreeMenu(GridContextMenuOpenEvent<File> contextEvent) {
		contextEvent.getContextMenu().removeItems();

		if (contextEvent.getItem() != null) {
			File f = (File) contextEvent.getItem();

			File dir = f.isDirectory() ? f : f.getParentFile();

			String dirAbbrev = StringUtils.abbreviate(dir.getName(), 12);
			String addCaption = "Create new file in '" + dirAbbrev + "'";
			contextEvent.getContextMenu().addItem(addCaption, VaadinIcons.FILE_ADD, (MenuItem selectedItem) -> {
				Window window = new Window("Creating a new file in '" + dirAbbrev + "'");

				TextField textField = new TextField("File name");
				textField.focus();
				textField.setWidth("100%");
				Button okButton = new Button("OK", e -> {
					window.close();
					createNewFile(dir, textField.getValue());
				});
				okButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
				okButton.setClickShortcut(KeyCode.ENTER, null);
				VerticalLayout windowRoot = new VerticalLayout(textField, okButton);
				windowRoot.setComponentAlignment(okButton, Alignment.BOTTOM_RIGHT);
				window.setContent(windowRoot);
				windowRoot.setSizeFull();
				windowRoot.setMargin(true);

				UI.getCurrent().addWindow(window);
				window.setWidth("400px");
				window.setHeight("200px");
				window.center();
				window.setResizable(false);
				window.setModal(true);

			});

			contextEvent.getContextMenu().addSeparator();

			String name = f.getName();
			String delCaption = "Delete '" + StringUtils.abbreviate(name, 12) + "'";
			VaadinIcons icon = f.isDirectory() ? VaadinIcons.FOLDER_REMOVE : VaadinIcons.FILE_REMOVE;
			MenuItem removeFile = contextEvent.getContextMenu().addItem(delCaption, icon, (MenuItem selectedItem) -> {
				if (f.isDirectory()) {
					try {
						FileUtils.deleteDirectory(f);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					f.delete();
				}
				// TODO re-init view without a navigation event
				UI.getCurrent().getNavigator().navigateTo(ViewIds.CONTAINER + "/" + dockerId);
			});
			removeFile.setStyleName(ValoTheme.BUTTON_DANGER);
		} else {
			MenuItem addItem = contextEvent.getContextMenu().addItem("N/A", VaadinIcons.BAN, selectedItem -> {
			});
			addItem.setEnabled(false);
		}

	}

	private void createNewFile(File dir, String value) {
		File file = new File(dir.getAbsolutePath() + "/" + value);
		String dockerPath = getFiddleDirectory().getAbsolutePath();
		if (!file.getAbsolutePath().startsWith(dockerPath)) {
			Notification.show("Error creating file", "Cannot create file outside of fiddle root", Type.ERROR_MESSAGE);
		}
		try {
			if (file.createNewFile()) {
				String dockerRelativePath = file.getAbsolutePath().substring(dockerPath.length());
				UI.getCurrent().getNavigator()
						.navigateTo(ViewIds.CONTAINER + "/" + dockerId + "/" + dockerRelativePath);
			} else {
				Notification.show("File already exists",
						"Could not create file ('" + StringUtils.abbreviate(value, 12) + "') ", Type.ERROR_MESSAGE);
			}
		} catch (IOException e) {
			Notification.show("Error creating file",
					"Could not create file ('" + StringUtils.abbreviate(value, 12) + "'): " + e.getMessage(),
					Type.ERROR_MESSAGE);
		}

	}

	private void updateTitle() {
		String file = selectedFile != null ? selectedFile.getName() + " - " : "";
		Page.getCurrent().setTitle(file + fiddleContainer.getName() + " - Container - VaadinFiddle");
	}

	/**
	 * 
	 * @param fileToSelect
	 *            relative path to file inside container
	 */
	private void expandAndSelectFile(String fileToSelect) {
		File fiddleDirectory = getFiddleDirectory();
		List<String> pathToFile = Arrays.asList(fileToSelect.split("/"));
		String parentPath = "";
		String absolutePath = fiddleDirectory.getAbsolutePath();
		for (String pathPart : pathToFile) {
			if (pathPart.isEmpty()) {
				continue;
			}
			String pathname = absolutePath + parentPath + "/" + pathPart;
			tree.expand(new File(pathname));
			parentPath += "/" + pathPart;
		}

		tree.select(new File(absolutePath + "/" + parentPath));

	}

	private void showFileInEditor(File selectedFile) {
		if (fileToEditorMap.get(selectedFile) != null) {
			editorTabs.setSelectedTab(fileToEditorMap.get(selectedFile));
			return;
		}
		Tab tab = createNewEditorTab(selectedFile);

		editorTabs.setSelectedTab(tab);
	}

	private Tab createNewEditorTab(File selectedFile) {

		CodeMirrorField codeMirrorField = new CodeMirrorField();

		String fileName = selectedFile.getName();
		codeMirrorField.setMode(FileTypeUtil.getMimeTypeByFileExtension(fileName));

		Binder<File> binder = new Binder<File>();
		BindingBuilder<File, String> bb = binder.forField(codeMirrorField);

		Binding<File, String> binding = bb.bind(new FileToStringValueProvider(), new StringToFileSetter());
		binder.readBean(selectedFile);

		codeMirrorField.setSizeFull();

		fileToEditorMap.put(selectedFile, codeMirrorField);
		fileToBinderMap.put(selectedFile, binder);

		Tab tab = editorTabs.addTab(codeMirrorField, fileName);
		String origValue = new FileToStringValueProvider().apply(selectedFile);

		codeMirrorField.addValueChangeListener(e -> {
			if (!codeMirrorField.getValue().equals(origValue)) {
				tab.setCaption(fileName + " *");
			} else {
				tab.setCaption(fileName);
			}
		});

		tab.setClosable(true);
		return tab;
	}

	private void expandAndSelectFirstJavaFile() {
		File fiddleDirectory = getFiddleDirectory();
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
			tree.expand(pathToJava.get(i));
		}

		tree.select(javaFile);
	}

	private File getFiddleDirectory() {
		return new File(fiddleContainer.getFiddleAppPath());
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

	private void createResultFrame() {
		URI location = Page.getCurrent().getLocation();
		String host = location.getHost();
		String scheme = location.getScheme();
		BrowserFrame frame = new BrowserFrame("",
				new ExternalResource(scheme + "://" + host + "/container/" + fiddleContainer.getId()));
		frame.setSizeFull();

		Panel resultPanel = new Panel("Fiddle result app", frame);
		frame.setSizeFull();
		resultPanel.setSizeFull();
		mainAreaAndFiddleResult.setSecondComponent(resultPanel);

		mainAreaAndFiddleResult.setSplitPosition(50, Unit.PERCENTAGE, true);

	}

	private void restartJetty() {
		Collection<Window> windows = new ArrayList<>(UI.getCurrent().getWindows());
		for (Window w : windows) {
			w.close();
		}

		mainAreaAndFiddleResult.setSecondComponent(null);
		mainAreaAndFiddleResult.setSplitPosition(100, Unit.PERCENTAGE);

		PanelOutput consoleOutput = createConsolePanel();
		FiddleUi.getDockerservice().restartJetty(fiddleContainer.getId(), consoleOutput, UI.getCurrent());
		readContainerInfo();
	}

	private PanelOutput createConsolePanel() {
		PanelOutput consoleOutput = new PanelOutput();
		consoleOutput.addJettyStartListener(() -> {
			editorTabs.getUI().access(() -> {
				createResultFrame();

				if (!startedMessageShown) {
					startedMessageShown = true;
					Notification notification = new Notification("Fiddle up n running",
							"Fiddle result is now running in the right panel. Try editing the code and hit save!",
							Type.TRAY_NOTIFICATION);
					notification.setDelayMsec(5000);
					notification.show(Page.getCurrent());
				}

			});
		});
		consoleOutput.addFirstLineReceivedListener(() -> {
			editorTabs.getUI().access(() -> editorTabsAndConsole.setSplitPosition(200, Unit.PIXELS, true));
		});
		consoleOutput.addErrorListener(() -> {
			editorTabs.getUI().access(() -> {

				Notification notification = new Notification("Error occurred", "Check the console output for more info",
						Type.WARNING_MESSAGE);
				notification.setDelayMsec(Notification.DELAY_FOREVER);
				notification.show(Page.getCurrent());

			});
		});
		Panel consolePanel = consoleOutput.getOutputPanel();
		consolePanel.setCaption("Console");
		editorTabsAndConsole.setSecondComponent(consolePanel);
		return consoleOutput;
	}

	private void saveAllFiles() {
		for (Component component : editorTabs) {
			File file = fileToEditorMap.inverse().get(component);
			fileToBinderMap.get(file);
			Binder<File> binder = fileToBinderMap.get(file);
			try {
				binder.writeBean(file);
			} catch (ValidationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			editorTabs.getTab(component).setCaption(file.getName());
		}
	}

}
