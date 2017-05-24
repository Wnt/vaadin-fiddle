package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.vaadin.data.ValueProvider;

public class FileToStringValueProvider implements ValueProvider<File, String> {
		@Override
		public String apply(File file) {
			// implementation from
			// com.vaadin.v7.data.util.TextFileProperty.getValue()
			try {
				FileInputStream fis = new FileInputStream(file);
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader r = new BufferedReader(isr);
				StringBuilder b = new StringBuilder();
				char buf[] = new char[8 * 1024];
				int len;
				while ((len = r.read(buf)) != -1) {
					b.append(buf, 0, len);
				}
				r.close();
				isr.close();
				fis.close();
				return b.toString();
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}