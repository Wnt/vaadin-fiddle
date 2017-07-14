package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.filefilter.WildcardFileFilter;

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
		case ".kt":
			return "text/x-kotlin";
		default:
			return "";
		}
	}

	public static File findFirstFileWithExtension(File directory, String extension) {
		FileFilter fileFilter = new WildcardFileFilter("*" + extension);
		File[] files = directory.listFiles(fileFilter);

		if (files.length > 0) {
			return files[0];
		} else {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					File subDirFile = findFirstFileWithExtension(file, extension);
					if (subDirFile != null) {
						return subDirFile;
					}
				}
			}
		}
		return null;

	}

}
