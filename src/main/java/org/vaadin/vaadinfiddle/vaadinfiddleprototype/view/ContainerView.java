package org.vaadin.vaadinfiddle.vaadinfiddleprototype.view;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.vaadin.addon.codemirror.CodeMirrorField;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleSession;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi.ViewIds;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.components.TreeWithContextMenu;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileSystemProvider;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileToStringValueProvider;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.FileTypeUtil;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.util.ShareDialog;
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
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.Extension;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ContainerView extends ContainerDesign implements View {

	private FiddleContainer fiddleContainer;
	private String dockerId;
	private BiMap<File, Component> fileToEditorMap = HashBiMap.create();
	private boolean startedMessageShown = false;
	private Map<File, Binder<File>> fileToBinderMap = new HashMap<>();
	private List<File> expandedDirectories = new ArrayList<>();
	private File selectedFile;
	private boolean userCanEdit;

	public ContainerView() {
		super();

		saveButton.addClickListener(e -> {
			onSaveClick();
		});

		shareButton.addClickListener(e -> {
			openShareDialog();
		});

		newButton.setDescription("Create a new fiddle (Alt + N)");
		newButton.addClickListener(e -> {
			getUI().getNavigator().navigateTo("");
		});

		initTree();

		initTabSheet();
	}

	@Override
	public void enter(ViewChangeEvent event) {
		String params = event.getParameters();

		int idFileSplit = params.indexOf("/");
		String fileToSelect = null;
		String requestedDockerId;
		if (idFileSplit != -1) {
			requestedDockerId = params.substring(0, idFileSplit);
			fileToSelect = params.substring(idFileSplit);
		} else {
			requestedDockerId = params;
		}

		userCanEdit = FiddleSession.getCurrent().ownsContainer(requestedDockerId);

		boolean containerChanged = !Objects.equals(dockerId, requestedDockerId);
		dockerId = requestedDockerId;

		if (containerChanged) {
			if (!userCanEdit) {
				showReadOnlyNotification(Type.TRAY_NOTIFICATION);
				saveButton.setEnabled(false);
				saveButton.setDescription("Please fork the fiddle to persist modifications.");
			}
			else {
				saveButton.setEnabled(true);
				saveButton.setDescription("Save all open files (Ctrl + Enter)");
			}
			editorTabs.removeAllComponents();
			fileToBinderMap.clear();
			fileToEditorMap.clear();

			Collection<Extension> forkExtensions = forkButton.getExtensions();
			for (Extension extension : new ArrayList<>(forkExtensions)) {
				forkButton.removeExtension(extension);
			}
			BrowserWindowOpener opener = new BrowserWindowOpener(getDeploymentURL() + "fork/" + dockerId);
			opener.extend(forkButton);

			readContainerInfo();
			updateTitle();

			populateTree();
			
			createResultFrame();
		}

		if (fileToSelect != null && !fileToSelect.isEmpty()) {
			expandAndSelectFile(fileToSelect);
		} else {
			expandAndSelectFirstJavaFile();
		}
	}

	private void initTabSheet() {
		// TODO use closehandler that checks for unsaved modifications
		editorTabs.setCloseHandler((tabsheet, tabContent) -> {
			File file = fileToEditorMap.inverse().get(tabContent);
			fileToEditorMap.remove(file);
			fileToBinderMap.remove(file);
			editorTabs.removeComponent(tabContent);
		});
		// TODO do in design
		editorTabs.setSizeFull();

		editorTabs.addSelectedTabChangeListener(selectionEvent -> {
			onEditorTabChange();
		});
	}

	private void onEditorTabChange() {
		File selectedFile = fileToEditorMap.inverse().get(editorTabs.getSelectedTab());

		String relativeFilePath = selectedFile.getAbsolutePath()
				.substring(fiddleContainer.getFiddleAppPath().length() + 1);

		String deploymentURL = getDeploymentURL();
		String permalink = deploymentURL + ViewIds.CONTAINER + "/" + dockerId + "/" + relativeFilePath;

		String currentLocation = Page.getCurrent().getLocation().toString();
		if (!currentLocation.startsWith(permalink)) {
			Page.getCurrent().pushState(permalink);
		}
		updateTitle();

		this.selectedFile = selectedFile;
		expandAndSelectFile(relativeFilePath);
		updateTitle();
	}

	/**
	 * Assumes URL is deploymentUrl + "/" + ViewIds.CONTAINER
	 * 
	 * @return e.g. https://vaadinfiddle.com/editor
	 */
	private String getDeploymentURL() {
		String currentLocation = Page.getCurrent().getLocation().toString();
		int deploymentPathEndIdx = currentLocation.indexOf(ViewIds.CONTAINER.toString());
		String deploymentURL = currentLocation.substring(0, deploymentPathEndIdx);
		return deploymentURL;
	}

	private void initTree() {
		getTree().setItemCaptionGenerator(File::getName);

		getTree().addExpandListener(expandEvent -> {
			expandedDirectories.add(expandEvent.getExpandedItem());
		});
		getTree().addCollapseListener(collapseEvent -> {
			expandedDirectories.remove(collapseEvent.getCollapsedItem());
		});

		getTree().setItemIconGenerator(file -> {
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

		addContextMenuToTree();

		getTree().addSelectionListener(e -> {
			onTreeSelection();
		});

		getTree().addExpandListener(e -> {
			File[] children = e.getExpandedItem().listFiles();
			if (children.length == 1) {
				for (File child : children) {
					getTree().expand(child);
				}
			}
		});
	}

	private void onTreeSelection() {
		File selectedFile = getTree().asSingleSelect().getValue();
		if (selectedFile == null || selectedFile.isDirectory()) {
			return;
		}
		showFileInEditor(selectedFile);
	}

	private void onSaveClick() {
		saveAllFiles();

		Notification notification = new Notification("Saving and restarting",
				"This shouldn't take longer than a few seconds", Type.TRAY_NOTIFICATION);
		notification.setDelayMsec(1000);
		notification.show(Page.getCurrent());
	}

	private void openShareDialog() {
		String currentLocation = Page.getCurrent().getLocation().toString();
		int deploymentPathEndIdx = currentLocation.indexOf(ViewIds.CONTAINER.toString());
		String deploymentURL = currentLocation.substring(0, deploymentPathEndIdx);
		Window window = new ShareDialog(fiddleContainer, getSelectedFileRelativePath(), deploymentURL);
		window.setModal(true);

		UI.getCurrent().addWindow(window);
		window.center();
		window.setWidth("850px");
		window.setHeight("560px");
	}

	private void populateTree() {
		expandedDirectories.clear();
		File fiddleDirectory = getFiddleDirectory();

		FileSystemProvider fp = new FileSystemProvider(fiddleDirectory);
		getTree().setDataProvider(fp);
	}

	@SuppressWarnings("unchecked")
	protected TreeWithContextMenu<File> getTree() {
		return tree;
	}

	private void addContextMenuToTree() {
		GridContextMenu<File> contextMenu = getTree().getContextMenu();
		contextMenu.addGridBodyContextMenuListener(this::onContextMenuOpen);
	}

	/**
	 * Updates tree's context menu items based on the GridContextMenuOpenEvent
	 * 
	 * @param contextEvent
	 */
	private void onContextMenuOpen(GridContextMenuOpenEvent<File> contextEvent) {
		contextEvent.getContextMenu().removeItems();

		if (contextEvent.getItem() != null) {
			File f = (File) contextEvent.getItem();

			getTree().select(f);

			File dir = f.isDirectory() ? f : f.getParentFile();
			String dirAbbrev = StringUtils.abbreviate(dir.getName(), 12);
			String addFileCaption = "Create new file in '" + dirAbbrev + "'";
			contextEvent.getContextMenu().addItem(addFileCaption, VaadinIcons.FILE_ADD, (MenuItem selectedItem) -> {
				createNewFileRequested(dir);

			});
			String addDirCaption = "Create new directory in '" + dirAbbrev + "'";
			contextEvent.getContextMenu().addItem(addDirCaption, VaadinIcons.FOLDER_ADD, (MenuItem selectedItem) -> {
				createNewDirRequested(dir);

			});

			contextEvent.getContextMenu().addSeparator();

			String name = f.getName();
			String delCaption = "Delete '" + StringUtils.abbreviate(name, 12) + "'";
			VaadinIcons icon = f.isDirectory() ? VaadinIcons.FOLDER_REMOVE : VaadinIcons.FILE_REMOVE;
			MenuItem removeFile = contextEvent.getContextMenu().addItem(delCaption, icon, (MenuItem selectedItem) -> {
				removeFileRequested(f);
			});
			removeFile.setStyleName(ValoTheme.BUTTON_DANGER);
		} else {
			MenuItem addItem = contextEvent.getContextMenu().addItem("N/A", VaadinIcons.BAN, selectedItem -> {
			});
			addItem.setEnabled(false);
		}

	}

	private void createNewDirRequested(File dir) {

		String dirAbbrev = StringUtils.abbreviate(dir.getName(), 12);
		Window window = new Window("Creating a new directory in '" + dirAbbrev + "'");

		TextField textField = new TextField("Directory name");
		textField.focus();
		textField.setWidth("100%");
		textField.setValueChangeMode(ValueChangeMode.EAGER);

		Button okButton = new Button("OK", e -> {
			window.close();
			createNewDir(dir, textField.getValue());
		});

		Binder<Void> binder = new Binder<>();
		binder.forField(textField)
				.withValidator(new StringLengthValidator("Name must be between 3 - 64 characters", 3, 64))
				.withValidationStatusHandler(validationEvent -> {
					okButton.setEnabled(!validationEvent.isError());
					if (validationEvent.getMessage().isPresent()) {
						okButton.setDescription(validationEvent.getMessage().get());
					} else {
						okButton.setDescription("Create directory");
					}
				}).bind((a) -> "", (s, d) -> {
				});
		// trigger validation event
		textField.setValue(" ");
		textField.setValue("");

		okButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		okButton.setClickShortcut(KeyCode.ENTER, null);
		VerticalLayout windowRoot = new VerticalLayout(textField, okButton);
		windowRoot.setComponentAlignment(okButton, Alignment.BOTTOM_RIGHT);
		window.setContent(windowRoot);
		windowRoot.setSizeFull();

		showDialogWindow(window);

	}

	private void createNewDir(File dir, String value) {
		File file = new File(dir.getAbsolutePath() + "/" + value);
		String dockerPath = getFiddleDirectory().getAbsolutePath();
		if (!file.getAbsolutePath().startsWith(dockerPath)) {
			Notification.show("Error creating directory", "Cannot create file outside of fiddle root",
					Type.ERROR_MESSAGE);
		}
		if (!file.mkdirs()) {
			Notification.show("Error creating directory", "Maybe the directory name is already in use?",
					Type.ERROR_MESSAGE);
		}
		List<File> oldExpands = new ArrayList<>(expandedDirectories);
		populateTree();
		getTree().expand(oldExpands);
	}

	private void removeFileRequested(File f) {
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
		populateTree();
		Component tab = fileToEditorMap.get(f);
		if (tab != null) {
			editorTabs.removeComponent(tab);
		}
		expandAndSelectFile(getSelectedFileRelativePath());
	}

	private String getSelectedFileRelativePath() {
		return selectedFile.getAbsolutePath().substring(getFiddleDirectory().getAbsolutePath().length());
	}

	/**
	 * Shows file name dialog to the user and creates the file on button click
	 * 
	 * @param dir
	 */
	private void createNewFileRequested(File dir) {

		String dirAbbrev = StringUtils.abbreviate(dir.getName(), 12);
		Window window = new Window("Creating a new file in '" + dirAbbrev + "'");

		TextField textField = new TextField("File name");
		textField.focus();
		textField.setWidth("100%");
		textField.setValueChangeMode(ValueChangeMode.EAGER);

		Button okButton = new Button("OK", e -> {
			window.close();
			createNewFile(dir, textField.getValue());
		});

		Binder<Void> binder = new Binder<>();
		binder.forField(textField)
				.withValidator(new StringLengthValidator("Name must be between 4 - 64 characters", 4, 64))
				.withValidationStatusHandler(validationEvent -> {
					okButton.setEnabled(!validationEvent.isError());
					if (validationEvent.getMessage().isPresent()) {
						okButton.setDescription(validationEvent.getMessage().get());
					} else {
						okButton.setDescription("Create file");
					}
				}).bind((a) -> "", (s, d) -> {
				});
		// trigger validation event
		textField.setValue(" ");
		textField.setValue("");

		okButton.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		okButton.setClickShortcut(KeyCode.ENTER, null);
		VerticalLayout windowRoot = new VerticalLayout(textField, okButton);
		windowRoot.setComponentAlignment(okButton, Alignment.BOTTOM_RIGHT);
		window.setContent(windowRoot);
		windowRoot.setSizeFull();

		showDialogWindow(window);
	}

	private static void showDialogWindow(Window window) {
		UI.getCurrent().addWindow(window);
		window.setWidth("400px");
		window.setHeight("200px");
		window.center();
		window.setResizable(false);
		window.setModal(true);
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

				List<File> oldExpands = new ArrayList<>(expandedDirectories);
				populateTree();
				getTree().expand(oldExpands);

				expandAndSelectFile(dockerRelativePath);

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
		String title = file + fiddleContainer.getName() + " - Container - VaadinFiddle";
		// workaround for https://github.com/vaadin/framework/issues/10280 (History
		// entries are saved with wrong title)
		Page.getCurrent().getJavaScript().execute("document.title = '" + title + "';");
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
			getTree().expand(new File(pathname));
			parentPath += "/" + pathPart;
		}

		getTree().select(new File(absolutePath + "/" + parentPath));

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

		// TODO set to read only if userCanEdit is false

		codeMirrorField.setSizeFull();

		fileToEditorMap.put(selectedFile, codeMirrorField);
		fileToBinderMap.put(selectedFile, binder);

		Tab tab = editorTabs.addTab(codeMirrorField, fileName);
		String origValue = new FileToStringValueProvider().apply(selectedFile);

		codeMirrorField.addValueChangeListener(e -> {
			if (!codeMirrorField.getValue().equals(origValue)) {
				if (userCanEdit) {
					tab.setCaption(fileName + " *");
				} else {
					showReadOnlyNotification();
				}
			} else {
				tab.setCaption(fileName);
			}
		});

		tab.setClosable(true);
		return tab;
	}

	private void showReadOnlyNotification() {
		Type type = Type.WARNING_MESSAGE;
		showReadOnlyNotification(type);
	}

	private void showReadOnlyNotification(Type type) {
		Notification forkNotif = new Notification("Fiddle open in read only mode", "Please <a href=\""
				+ getDeploymentURL() +  "/" + dockerId + "\">fork it</a> to persist modifications.", type,
				true);
		forkNotif.setDelayMsec(1000000);
		forkNotif.show(Page.getCurrent());
	}

	private void expandAndSelectFirstJavaFile() {
		File fiddleDirectory = getFiddleDirectory();
		File codeFile = FileTypeUtil.findFirstFileWithExtension(fiddleDirectory, ".java");
		if (codeFile == null) {
			codeFile = FileTypeUtil.findFirstFileWithExtension(fiddleDirectory, ".kt");
		}
		if (codeFile == null) {
			return;
		}

		ArrayList<File> pathToJava = new ArrayList<>();

		File fs = codeFile;

		while (!fs.getParentFile().equals(fiddleDirectory)) {
			pathToJava.add(fs.getParentFile());
			fs = fs.getParentFile();
		}
		for (int i = pathToJava.size() - 1; i >= 0; i--) {
			tree.expand(pathToJava.get(i));
		}

		tree.select(codeFile);
	}

	private File getFiddleDirectory() {
		return fiddleContainer.getFiddleDirectory();
	}

	private void readContainerInfo() {
		fiddleContainer = new FiddleContainer() {
			
			@Override
			public String getName() {
				return "Test Container";
			}
			
			@Override
			public String getId() {
				return "1";
			}
			
			@Override
			public File getFiddleDirectory() {
				return new File(getFiddleAppPath());
			}
			
			@Override
			public String getFiddleAppPath() {
				return System.getProperty("fiddle.directory");
			}
		};
	}

	private void createResultFrame() {
		String frameUrl = getFrameUrl(fiddleContainer);
		BrowserFrame frame = new BrowserFrame("", new ExternalResource(frameUrl));
		frame.setSizeFull();

		Panel resultPanel = new Panel("Fiddle result app", frame);
		frame.setSizeFull();
		resultPanel.setSizeFull();
		mainAreaAndFiddleResult.setSecondComponent(resultPanel);

		mainAreaAndFiddleResult.setSplitPosition(50, Unit.PERCENTAGE, true);

	}

	private static String getFrameUrl(FiddleContainer fiddleContainer) {
		URI location = Page.getCurrent().getLocation();
		String host = location.getHost();
		String scheme = location.getScheme();
		String frameUrl = scheme + "://" + host + "/container/" + fiddleContainer.getId();
		return frameUrl;
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
