package org.vaadin.vaadinfiddle.vaadinfiddleprototype;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.server.VaadinServlet;

/**
 * The File servlet for serving from absolute path.
 * 
 * @author BalusC
 * @link http://balusc.blogspot.com/2007/07/fileservlet.html
 */
@WebServlet("/preview-image/*")
public class PreviewImageServlet extends HttpServlet {

	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

	private String resourcePath;

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		resourcePath = getResourcePath(servletContext, "/preview-image");

	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Get requested file by path info.
		String requestedFile = request.getPathInfo();

		// Check if file is actually supplied to the request URI.
		if (requestedFile == null) {
			// Do your thing if the file is not supplied to the request URI.
			// Throw an exception, or send 404, or show default/warning page, or just ignore
			// it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		File file = new File(resourcePath, requestedFile);

		// Check if file actually exists in filesystem.
		if (!file.exists()) {
			// Do your thing if the file appears to be non-existing.
			// Throw an exception, or send 404, or show default/warning page, or just ignore
			// it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		// Get content type by filename.
		String contentType = getServletContext().getMimeType(file.getName());

		// If content type is unknown, then set the default value.
		// For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		// Init servlet response.
		response.reset();
		response.setBufferSize(DEFAULT_BUFFER_SIZE);
		response.setContentType(contentType);
		response.setHeader("Content-Length", String.valueOf(file.length()));
		// response.setHeader("Content-Disposition", "attachment; filename=\"" +
		// file.getName() + "\"");

		try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
				BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream(),
						DEFAULT_BUFFER_SIZE);) {

			// Write file contents to response.
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
		}
	}

	/**
	 * Copied from {@link VaadinServlet#getResourcePath(ServletContext)}
	 * 
	 * @param servletContext
	 * @param path
	 *            the resource path.
	 * @return the resource path.
	 */
	public static String getResourcePath(ServletContext servletContext, String path) {
		String resultPath = null;
		resultPath = servletContext.getRealPath(path);
		if (resultPath != null) {
			return resultPath;
		} else {
			try {
				final URL url = servletContext.getResource(path);
				resultPath = url.getFile();
			} catch (final Exception e) {
				// FIXME: Handle exception
				getLogger().log(Level.INFO, "Could not find resource path " + path, e);
			}
		}
		return resultPath;
	}

	private static final Logger getLogger() {
		return Logger.getLogger(PreviewImageServlet.class.getName());
	}

}