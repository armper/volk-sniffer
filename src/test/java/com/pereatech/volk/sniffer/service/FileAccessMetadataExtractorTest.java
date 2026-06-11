package com.pereatech.volk.sniffer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pereatech.volk.sniffer.model.SearchFile;

class FileAccessMetadataExtractorTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void capturesTheEffectiveIndexerIdentityAndOwner() throws Exception {
		Path file = temporaryDirectory.resolve("access-test.txt");
		Files.writeString(file, "permission test");
		SearchFile searchFile = new SearchFile();

		new FileAccessMetadataExtractor().extract(file, temporaryDirectory, searchFile);

		assertThat(searchFile.getIndexerUser()).isEqualTo(System.getProperty("user.name"));
		assertThat(searchFile.getFileOwner()).isNotBlank();
		assertThat(searchFile.isIndexerReadable()).isTrue();
		assertThat(searchFile.getAccessControlSource()).isNotBlank();
	}
}
