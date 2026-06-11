package com.pereatech.volk.sniffer.filters;

import java.util.Arrays;

import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * Accepts only the file extensions configured under
 * {@code volk.sniffer.file-types}, and skips MS Office lock files.
 */
public class IndexedFileFilter<T> implements GenericFileFilter<T> {

	@Value("${volk.sniffer.file-types}")
	private String[] fileTypes;

	@Override
	public boolean accept(GenericFile<T> file) {
		if (file.isDirectory()) {
			return true;
		}

		String fileName = file.getFileName();
		// "~$..." files are MS Office lock files left behind by open documents
		if (fileName.startsWith("~$")) {
			return false;
		}

		String extension = FilenameUtils.getExtension(fileName);
		return Arrays.stream(fileTypes).anyMatch(type -> type.equalsIgnoreCase(extension));
	}
}
