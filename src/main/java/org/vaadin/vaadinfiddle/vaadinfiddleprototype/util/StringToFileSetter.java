package org.vaadin.vaadinfiddle.vaadinfiddleprototype.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.vaadin.server.Setter;

public class StringToFileSetter implements Setter<File, String> {
		@Override
		public void accept(File file, String value) {
			// implementation from
			// com.vaadin.v7.data.util.TextFileProperty.setValue()
			try {
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				BufferedWriter w = new BufferedWriter(osw);
				w.append(value);
				w.flush();
				w.close();
				osw.close();
				fos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}