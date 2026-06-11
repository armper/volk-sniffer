package com.pereatech.volk.sniffer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.WatchFolder;

class DocumentProvenanceExtractorTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void appliesTheSourceProfileAndRelativeLocation() throws Exception {
		Path nested = Files.createDirectories(temporaryDirectory.resolve("reports/annual"));
		Path file = Files.writeString(nested.resolve("results.txt"), "results");
		SearchFile searchFile = new SearchFile();
		searchFile.setAuthor("Document Author");
		searchFile.setFileOwner("filesystem-owner");
		WatchFolder source = new WatchFolder(temporaryDirectory.toString(), true, "source-1", "Finance records",
				"SHARED_DRIVE", "Finance", "Controller team");

		new DocumentProvenanceExtractor().extract(file, source, searchFile);

		assertThat(searchFile.getSourceName()).isEqualTo("Finance records");
		assertThat(searchFile.getSourceType()).isEqualTo("SHARED_DRIVE");
		assertThat(searchFile.getRelativePath()).isEqualTo("reports/annual/results.txt");
		assertThat(searchFile.getContentOwner()).isEqualTo("Controller team");
		assertThat(searchFile.getOwnershipBasis()).isEqualTo("SOURCE_PROFILE");
		assertThat(searchFile.getDepartment()).isEqualTo("Finance");
		assertThat(searchFile.getSourceAccessSummary()).contains("owner=");
	}

	@Test
	void fallsBackToEmbeddedAuthorWhenTheSourceHasNoAssignedOwner() throws Exception {
		Path file = Files.writeString(temporaryDirectory.resolve("author.txt"), "author");
		SearchFile searchFile = new SearchFile();
		searchFile.setAuthor("Embedded Author");
		searchFile.setFileOwner("filesystem-owner");
		WatchFolder source = new WatchFolder(temporaryDirectory.toString(), true, "source-2", "Documents",
				"LOCAL_FOLDER", "", "");

		new DocumentProvenanceExtractor().extract(file, source, searchFile);

		assertThat(searchFile.getContentOwner()).isEqualTo("Embedded Author");
		assertThat(searchFile.getOwnershipBasis()).isEqualTo("DOCUMENT_METADATA");
	}
}
