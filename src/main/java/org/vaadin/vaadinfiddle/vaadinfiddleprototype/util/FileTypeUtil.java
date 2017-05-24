package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

public class FileTypeUtil {
	public static String getMimeTypeByFileExtension(String name) {
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

}
