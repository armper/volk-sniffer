package com.pereatech.volk.sniffer.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.pereatech.volk.sniffer.model.SearchFile;

/**
 * Extracts document metadata (title, author, keywords, ...) and text content
 * from any file format Apache Tika understands (MS Office, PDF, plain text,
 * and many more).
 */
@Service
public class TikaMetadataExtractor {

	/** Cap on extracted text, so huge documents don't blow up MongoDB. */
	static final int MAX_CONTENT_CHARS = 100_000;

	private final AutoDetectParser parser = new AutoDetectParser();

	public void extract(InputStream is, SearchFile target) throws IOException, SAXException, TikaException {
		BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_CHARS);
		Metadata metadata = new Metadata();

		try {
			parser.parse(is, handler, metadata, new ParseContext());
		} catch (WriteLimitReachedException e) {
			// content was truncated at MAX_CONTENT_CHARS - keep what we have
		}

		target.setTitle(clean(metadata.get(TikaCoreProperties.TITLE)));
		target.setAuthor(clean(metadata.get(TikaCoreProperties.CREATOR)));
		target.setKeywords(clean(String.join(", ", metadata.getValues(TikaCoreProperties.SUBJECT))));
		target.setComments(clean(metadata.get(TikaCoreProperties.DESCRIPTION)));
		target.setContentType(clean(metadata.get(Metadata.CONTENT_TYPE)));
		target.setContent(clean(handler.toString()));
	}

	private static String clean(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}
}
