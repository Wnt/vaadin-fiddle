package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.v7.data.util.FilesystemContainer;
import com.vaadin.v7.ui.Tree;

public class ContainerView extends CustomComponent implements View {

	private TabSheet editorTabs;
	private FiddleContainer fiddleContainer;
	private Window fiddleWindow;
	private HorizontalLayout toolbar;
	private String dockerId;
	private Map<File, FileEditor> fileToEditorMap = new HashMap<>();

	@SuppressWarnings("deprecation")
	@Override
	public void enter(ViewChangeEvent event) {
		setSizeFull();

		dockerId = event.getParameters();
		HorizontalSplitPanel editorSplit = new HorizontalSplitPanel();
		toolbar = new HorizontalLayout();
		VerticalLayout rootLayout = new VerticalLayout(toolbar, editorSplit);
		setCompositionRoot(rootLayout);
		rootLayout.setExpandRatio(editorSplit, 1);
		rootLayout.setSizeFull();

		editorSplit.setSizeFull();

		editorSplit.setSplitPosition(250, Unit.PIXELS);

		Button saveButton = new Button(FontAwesome.SAVE);
		saveButton.setDescription("Save all");
		saveButton.addClickListener(e -> {
			saveAllFiles();
			restartJetty();
		});
		toolbar.addComponent(saveButton);

		readContainerInfo();
		Page.getCurrent().setTitle(fiddleContainer.getName() + " - Container - VaadinFiddle");

		FiddleUi.getDockerservice().setOwner(dockerId, UI.getCurrent());

		File fiddleDirectory = new File(fiddleContainer.getFiddleAppPath());

		FilesystemContainer f = new FilesystemContainer(fiddleDirectory);
		Tree tree = new Tree("", f);
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
		} else {
			Notification.show("Looks like your fiddle is not currently running. Save to start it",
					Notification.Type.TRAY_NOTIFICATION);
		}
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
		for (Window w : UI.getCurrent().getWindows()) {
			w.close();
		}
		WindowOutput consoleOutput = new WindowOutput();
		consoleOutput.addJettyStartListener(() -> {
			editorTabs.getUI().access(new Runnable() {

				@Override
				public void run() {
					createFiddleWindow();

				}
			});
		});
		FiddleUi.getDockerservice().restartJetty(fiddleContainer.getId(), consoleOutput, UI.getCurrent());
		readContainerInfo();
	}

	private void saveAllFiles() {
		for (Component component : editorTabs) {
			FileEditor fileEditor = (FileEditor) component;
			fileEditor.commit();
			editorTabs.getTab(fileEditor).setCaption(fileEditor.getFile().getName());
		}
	}

}
