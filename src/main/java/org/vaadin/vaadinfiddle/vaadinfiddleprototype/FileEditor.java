package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.vaadin.addon.codemirror.CodeMirrorField;

import com.vaadin.ui.Component;
import com.vaadin.v7.data.util.TextFileProperty;
import com.vaadin.v7.event.FieldEvents.TextChangeEvent;
import com.vaadin.v7.event.FieldEvents.TextChangeListener;
import com.vaadin.v7.ui.CustomField;

@SuppressWarnings("deprecation")
public final class FileEditor extends CustomField<String> {
	final private File file;
	private List<TextChangeListener> textChangeListeners = new ArrayList<>();
	private CodeMirrorField cm;

	public FileEditor(File file) {
		this.file = file;
		
		cm = new CodeMirrorField();
		cm.setMode(getApplicableMode());
		cm.setSizeFull();
		addStyleName("file-editor");

		cm.addValueChangeListener(e -> {
			for (TextChangeListener textChangeListener : textChangeListeners) {
				textChangeListener.textChange(new TextChangeEvent(this) {

					@Override
					public String getText() {
						return cm.getValue();
					}

					@Override
					public int getCursorPosition() {
						return 0;
					}
				});
			}
			setValue(cm.getValue());
		});
		TextFileProperty tf = new TextFileProperty(file);
		setPropertyDataSource(tf);
		setBuffered(true);
	}

	private String getApplicableMode() {
		String name = file.getName();
		int lastIndexOf = name.lastIndexOf('.');
		if (lastIndexOf == -1) {
			return "";
		}
		String fileExtension = name.substring(lastIndexOf);
		switch (fileExtension) {
		case ".java":
			return "text/x-java";
		case ".xml":
			return "application/xml";
		case ".md":
			return "text/x-markdown";
		case ".css":
			return "text/css";
		case ".scss":
			return "text/x-scss";
		case ".js":
			return "text/javascript";
		default:
			return "";
		}
	}

	public File getFile() {
		return file;
	}

	public void addTextChangeListener(TextChangeListener l) {
		textChangeListeners.add(l);
	}

	@Override
	protected Component initContent() {
		return cm;
	}

	@Override
	protected void setInternalValue(String newValue) {
		super.setInternalValue(newValue);
		cm.setValue(newValue);
	}

	@Override
	public Class getType() {
		return String.class;
	}
}