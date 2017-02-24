package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.File;
import java.util.Collection;

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
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.v7.data.util.FilesystemContainer;
import com.vaadin.v7.data.util.TextFileProperty;
import com.vaadin.v7.ui.TextArea;
import com.vaadin.v7.ui.Tree;

public class ContainerView extends CustomComponent implements View {

	private final class FileEditor extends TextArea {
		final private File file;

		public FileEditor(File file) {
			this.file = file;
			TextFileProperty tf = new TextFileProperty(file);
			setPropertyDataSource(tf);
			setBuffered(true);
			addStyleName("file-editor");
		}

		public File getFile() {
			return file;
		}
	}

	private TabSheet editorTabs;
	private FiddleContainer fiddleContainer;
	private Window fiddleWindow;
	private HorizontalLayout toolbar;
	private String dockerId;

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

		File fiddleDirectory = new File(fiddleContainer.getFiddleAppPath());

		FilesystemContainer f = new FilesystemContainer(fiddleDirectory);
		Tree tree = new Tree("", f);
		editorSplit.setFirstComponent(tree);
		tree.setSizeFull();
		editorTabs = new TabSheet();

		// TODO use closehandler that checks for unsaved modifications
		// editorTabs.setCloseHandler((tabsheet, tabContent) -> {
		//
		//
		// });
		editorSplit.setSecondComponent(editorTabs);
		editorTabs.setSizeFull();

		Collection<String> containerPropertyIds = f.getContainerPropertyIds();

		tree.setItemCaptionPropertyId("Name");

		tree.addValueChangeListener(e -> {
			File selectedFile = (File) tree.getValue();
			FileEditor fileEditor = new FileEditor(selectedFile);
			fileEditor.setSizeFull();

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

		createFiddleWindow();
	}

	private void readContainerInfo() {
		InspectContainerResponse containerInfo = FiddleUi.getDockerservice().getDockerClient().inspectContainerCmd(dockerId)
				.exec();
		fiddleContainer = new FiddleContainer(containerInfo);
	}

	private void createFiddleWindow() {
		String host = Page.getCurrent().getLocation().getHost();
		BrowserFrame frame = new BrowserFrame("", new ExternalResource(
				"http://" + host + ":" + fiddleContainer.getFiddlePort()));
		frame.setSizeFull();
		fiddleWindow = new Window("Fiddle app", frame);
		fiddleWindow.setWidth("800px");
		fiddleWindow.setHeight("800px");
		fiddleWindow.setPositionX(Page.getCurrent().getBrowserWindowWidth() - 830);
		fiddleWindow.setPositionY(30);
		
		fiddleWindow.setClosable(false);

		UI.getCurrent().addWindow(fiddleWindow);
	}

	private void restartJetty() {
		WindowOutput consoleOutput = new WindowOutput();
		consoleOutput.addJettyStartListener(() -> {
			editorTabs.getUI().access(new Runnable() {
				
				@Override
				public void run() {
					createFiddleWindow();
					
				}
			});
		});
		FiddleUi.getDockerservice().restartJetty(fiddleContainer.getId(),consoleOutput);
		readContainerInfo();
		fiddleWindow.close();
	}

	private void saveAllFiles() {
		for (Component component : editorTabs) {
			FileEditor fileEditor = (FileEditor) component;
			fileEditor.commit();
			editorTabs.getTab(fileEditor).setCaption(fileEditor.getFile().getName());
		}
	}

}
