package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import org.vaadin.vaadinfiddle.vaadinfiddleprototype.FiddleUi.ViewIds;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.components.PreviewImage;
import org.vaadin.vaadinfiddle.vaadinfiddleprototype.data.FiddleContainer;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class ShareDialog extends Window {

	private FiddleContainer fiddleContainer;
	private String selectedFile;
	private String frameURL;
	private String deploymentURL;
	private String editorUiURL;

	public ShareDialog(FiddleContainer fiddleContainer, String selectedFile, String deploymentURL) {
		super("Share preview");
		this.fiddleContainer = fiddleContainer;
		this.selectedFile = selectedFile;
		this.deploymentURL = deploymentURL;
		createUrls();
		TabSheet tabs = new TabSheet();

		Component bbcode = createBBCodeTab();
		Component embed = createEmbedTab();
		Component url = createUrlTab();

		tabs.addTab(bbcode).setDescription("Usable on most discussion forums etc.");
		tabs.addTab(embed).setDescription("In-line editable. Usable on blogs, webpages.");
		tabs.addTab(url)
				.setDescription("Usable anywhere! Best option when sharing on social media / instant messaging.");

		tabs.setSizeFull();
		setContent(tabs);
	}

	private TextArea createUrlTab() {
		TextArea url = new TextArea("URL", frameURL);
		url.setRows(2);
		url.setWidth("100%");
		return url;
	}

	private void createUrls() {
		frameURL = deploymentURL + ViewIds.PREVIEW + "/" + fiddleContainer.getId() + selectedFile;
		editorUiURL = deploymentURL + ViewIds.CONTAINER + "/" + fiddleContainer.getId() + selectedFile;
	}

	private VerticalLayout createBBCodeTab() {
		String imageName = selectedFile + ".png";
		imageName = fiddleContainer.getId() + imageName.replace('/', '_');

		String imgURL = deploymentURL + "preview-image/" + imageName;
		TextArea bbcodeField = new TextArea(null, "[url=" + editorUiURL + "][img]" + imgURL + "[/img]\n" + 
				"See the full sample on vaadinfiddle.com[/url]");
		bbcodeField.setRows(2);
		bbcodeField.setWidth("100%");

		Component imgPreview = new PreviewImage(frameURL, imageName);
		imgPreview.setCaption("Preview");

		imgPreview.setWidth("800px");
		imgPreview.setHeight("355px");
		VerticalLayout bbcode = new VerticalLayout(bbcodeField, imgPreview);
		bbcode.setSizeFull();
		bbcode.setExpandRatio(imgPreview, 1);
		bbcode.setComponentAlignment(imgPreview, Alignment.MIDDLE_CENTER);
		bbcode.setCaption("BBCode");
		return bbcode;
	}

	private VerticalLayout createEmbedTab() {
		BrowserFrame frame = new BrowserFrame("Preview", new ExternalResource(frameURL));

		frame.setWidth("800px");
		frame.setHeight("355px");
		TextArea iframeField = new TextArea(null,
				"<iframe src=\"" + frameURL + "\" width=\"560\" height=\"315\" frameborder=\"0\"></iframe> ");
		iframeField.setRows(2);
		iframeField.setWidth("100%");
		VerticalLayout embed = new VerticalLayout(iframeField, frame);
		embed.setSizeFull();
		embed.setExpandRatio(frame, 1);
		embed.setComponentAlignment(frame, Alignment.MIDDLE_CENTER);
		embed.setCaption("Embed");
		return embed;
	}
}