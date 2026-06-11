package com.pereatech.volk.sniffer.filters;

import java.util.Arrays;

import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;

public class OfficeDocumentFilter<T> implements GenericFileFilter<T> {

	@Value("${file-types.office-document}")
	private String[] officeDocumentTypes;

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
		return Arrays.stream(officeDocumentTypes).anyMatch(type -> type.equalsIgnoreCase(extension));
	}
}
