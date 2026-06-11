package com.pereatech.volk.sniffer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.pereatech.volk.sniffer.model.SearchFile;

class TikaMetadataExtractorTest {

	private final TikaMetadataExtractor extractor = new TikaMetadataExtractor();

	@Test
	void extractsMetadataAndContentFromDocx() throws Exception {
		SearchFile searchFile = new SearchFile();

		extractor.extract(new ByteArrayInputStream(minimalDocx()), searchFile);

		assertThat(searchFile.getTitle()).isEqualTo("Volk Test Document");
		assertThat(searchFile.getAuthor()).isEqualTo("Armando Perea");
		assertThat(searchFile.getKeywords()).contains("volk");
		assertThat(searchFile.getContent()).contains("Hello Volk");
		assertThat(searchFile.getContentType()).contains("wordprocessingml");
	}

	@Test
	void extractsContentFromPlainText() throws Exception {
		SearchFile searchFile = new SearchFile();

		extractor.extract(
				new ByteArrayInputStream("needle in a haystack".getBytes(StandardCharsets.UTF_8)), searchFile);

		assertThat(searchFile.getContent()).contains("needle in a haystack");
		assertThat(searchFile.getContentType()).contains("text/plain");
	}

	private byte[] minimalDocx() throws Exception {
		String contentTypes = """
				<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
				<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
				<Default Extension="xml" ContentType="application/xml"/>
				<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
				<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
				</Types>""";
		String rels = """
				<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
				<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
				<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
				</Relationships>""";
		String document = """
				<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>Hello Volk</w:t></w:r></w:p></w:body></w:document>""";
		String core = """
				<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
				<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
				<dc:title>Volk Test Document</dc:title><dc:creator>Armando Perea</dc:creator>
				<cp:keywords>volk,test</cp:keywords><dc:description>A test doc</dc:description>
				</cp:coreProperties>""";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(out)) {
			for (String[] entry : new String[][] {
					{ "[Content_Types].xml", contentTypes },
					{ "_rels/.rels", rels },
					{ "word/document.xml", document },
					{ "docProps/core.xml", core } }) {
				zip.putNextEntry(new ZipEntry(entry[0]));
				zip.write(entry[1].getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return out.toByteArray();
	}
}
