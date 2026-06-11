package com.pereatech.volk.sniffer.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.camel.component.file.GenericFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IndexedFileFilterTest {

	private final IndexedFileFilter<File> filter = new IndexedFileFilter<>();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(filter, "fileTypes", new String[] { "docx", "pdf", "txt" });
	}

	@Test
	void acceptsConfiguredExtensions() {
		assertThat(filter.accept(file("report.docx"))).isTrue();
		assertThat(filter.accept(file("REPORT.PDF"))).isTrue();
	}

	@Test
	void rejectsOtherExtensions() {
		assertThat(filter.accept(file("video.mp4"))).isFalse();
		assertThat(filter.accept(file("archive.zip"))).isFalse();
		// "docx" must match the extension, not a suffix of the name
		assertThat(filter.accept(file("notadocx"))).isFalse();
	}

	@Test
	void rejectsOfficeLockFiles() {
		assertThat(filter.accept(file("~$report.docx"))).isFalse();
	}

	@Test
	void acceptsDirectoriesForTraversal() {
		GenericFile<File> dir = file("subdir");
		dir.setDirectory(true);
		assertThat(filter.accept(dir)).isTrue();
	}

	private GenericFile<File> file(String name) {
		GenericFile<File> genericFile = new GenericFile<>();
		genericFile.setFileName(name);
		genericFile.setDirectory(false);
		return genericFile;
	}
}
