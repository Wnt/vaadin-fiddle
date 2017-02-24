package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.File;
import java.util.Collection;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.util.FilesystemContainer;
import com.vaadin.v7.data.util.TextFileProperty;
import com.vaadin.v7.ui.TextArea;
import com.vaadin.v7.ui.Tree;

public class ContainerView extends CustomComponent implements View {

	@Override
	public void enter(ViewChangeEvent event) {
		setSizeFull();

		String p = event.getParameters();
		HorizontalSplitPanel editorSplit = new HorizontalSplitPanel();
		HorizontalLayout toolbar = new HorizontalLayout();
		VerticalLayout rootLayout = new VerticalLayout(toolbar, editorSplit);
		setCompositionRoot(rootLayout);
		rootLayout.setExpandRatio(editorSplit, 1);
		rootLayout.setSizeFull();

		editorSplit.setSizeFull();

		editorSplit.setSplitPosition(250, Unit.PIXELS);

		Button saveButton = new Button(FontAwesome.SAVE);
		saveButton.setDescription("Save all");
		saveButton.addClickListener(e -> {
			
		});
		toolbar.addComponent(saveButton);

		InspectContainerResponse containerInfo = FiddleUi.getDockerservice().getDockerClient().inspectContainerCmd(p)
				.exec();

		Page.getCurrent().setTitle(containerInfo.getName() + " - Container - VaadinFiddle");

		FiddleContainer fiddleContainer = new FiddleContainer(containerInfo);
		File fiddleDirectory = new File(fiddleContainer.getFiddleAppPath());

		FilesystemContainer f = new FilesystemContainer(fiddleDirectory);
		Tree tree = new Tree("", f);
		editorSplit.setFirstComponent(tree);
		tree.setSizeFull();
		TabSheet editorTabs = new TabSheet();
		editorSplit.setSecondComponent(editorTabs);
		editorTabs.setSizeFull();
		
		Collection<String> containerPropertyIds = f.getContainerPropertyIds();

		tree.setItemCaptionPropertyId("Name");

		tree.addValueChangeListener(e -> {
			File selectedFile = (File) tree.getValue();
			TextFileProperty tf = new TextFileProperty(selectedFile);
			TextArea textEditor = new TextArea(tf);
			textEditor.setSizeFull();
			textEditor.setBuffered(true);

			String fileName = selectedFile.getName();
			Tab tab = editorTabs.addTab(textEditor, fileName);
			textEditor.addTextChangeListener(te -> {
				if (!te.getText().equals(textEditor.getPropertyDataSource().getValue())) {
					tab.setCaption(fileName+" *");
				}
				else {
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
	}

}
