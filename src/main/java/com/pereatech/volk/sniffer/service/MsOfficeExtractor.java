package com.pereatech.volk.sniffer.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.pereatech.volk.sniffer.model.OfficeDocument;
import com.pereatech.volk.sniffer.model.SearchFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts document metadata (title, author, keywords, ...) from MS Office
 * files using Apache POI.
 */
@Slf4j
public class MsOfficeExtractor {

	/**
	 * Reads metadata from legacy OLE2 documents (.doc, .xls, .ppt).
	 */
	public SearchFile getFromOle2(InputStream is) throws IOException {
		OfficeDocument document = new OfficeDocument();

		try (POIFSFileSystem fs = new POIFSFileSystem(is)) {
			SummaryInformation summary = (SummaryInformation) PropertySetFactory.create(fs.getRoot(),
					SummaryInformation.DEFAULT_STREAM_NAME);

			document.setTitle(summary.getTitle());
			document.setAuthor(summary.getAuthor());
			document.setKeywords(summary.getKeywords());
			document.setComments(summary.getComments());
			document.setCreateDateTime(Objects.toString(summary.getCreateDateTime(), null));
			document.setLastSaveDateTime(Objects.toString(summary.getLastSaveDateTime(), null));
		} catch (FileNotFoundException | NoPropertySetStreamException e) {
			log.debug("Document has no summary information stream: {}", e.getMessage());
		}

		return document;
	}

	/**
	 * Reads metadata from OOXML documents (.docx, .xlsx, .pptx, ...).
	 */
	public SearchFile getFromOoxml(InputStream is) throws IOException {
		OfficeDocument document = new OfficeDocument();

		try (OPCPackage pkg = OPCPackage.open(is)) {
			POIXMLProperties.CoreProperties core = new POIXMLProperties(pkg).getCoreProperties();

			document.setTitle(core.getTitle());
			document.setAuthor(core.getCreator());
			document.setKeywords(core.getKeywords());
			document.setComments(core.getDescription());
			document.setCreateDateTime(Objects.toString(core.getCreated(), null));
			document.setLastSaveDateTime(Objects.toString(core.getModified(), null));
		} catch (Exception e) {
			throw new IOException("Unable to read OOXML metadata: " + e.getMessage(), e);
		}

		return document;
	}
}
