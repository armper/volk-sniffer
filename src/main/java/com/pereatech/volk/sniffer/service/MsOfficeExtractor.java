package com.pereatech.volk.sniffer.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.io.IOUtils;
import org.apache.poi.POIXMLProperties;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.*;

import com.pereatech.volk.sniffer.model.OfficeDocument;
import com.pereatech.volk.sniffer.model.SearchFile;

public class MsOfficeExtractor {

	public class MetaDataListener implements POIFSReaderListener {
		public final Map<String, Object> metaData;

		public MetaDataListener() {
			metaData = new HashMap<String, Object>();
		}

		public void processPOIFSReaderEvent(final POIFSReaderEvent event) {
			SummaryInformation summaryInformation = null;
			try {
				summaryInformation = (SummaryInformation) PropertySetFactory.create(event.getStream());
			} catch (NoPropertySetStreamException | MarkUnsupportedException | IOException e) {
				throw new RuntimeException("Error creating summary information  "+e.getMessage());
			}
			Object propertyValue = null;

			for (int i = 0; i < properties.length; i++) {
				Method method = (Method) methodMap.get(properties[i]);
				try {
					propertyValue = method.invoke(summaryInformation, (Object[]) (Object[]) null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Error invoking property value "+e.getMessage());
				}

				metaData.put(properties[i], propertyValue);
			}

		}
	}

	private String[] properties;

	private Map<String, Method> methodMap;

	public MsOfficeExtractor() {

		String[] poiProperties = new String[] { "Title", "Author", "Keywords", "Comments", "CreateDateTime",
				"LastSaveDateTime" };

		this.properties = (poiProperties == null ? new String[] {} : poiProperties);
		methodMap = new HashMap<String, Method>();
		try {
			for (int i = 0; i < properties.length; i++) {
				methodMap.put(properties[i], SummaryInformation.class.getMethod("get" + properties[i], (Class[]) null));
			}
		} catch (SecurityException e) {
			// error handling
		} catch (NoSuchMethodException e) {
			// error handling
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseMetaData(final byte[] fileData) {
		if (properties.length == 0) {
			return Collections.EMPTY_MAP;
		}

		MetaDataListener metaDataListener = new MetaDataListener();

		InputStream in = null;
		try {
			in = new ByteArrayInputStream(fileData);
			POIFSReader poifsReader = new POIFSReader();
			poifsReader.registerListener(metaDataListener, "\005SummaryInformation");
			poifsReader.read(in);

		} catch (final IOException e) {
			// error handling
		} catch (final RuntimeException e) {
			// error handling
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}
		return metaDataListener.metaData;
	}

	public SearchFile getFromOffice97(InputStream is) {
		byte[] fileData = null;
		try {
			fileData = IOUtils.toByteArray(is);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

		Map<String, Object> metadata = this.parseMetaData(fileData);

		OfficeDocument officeDocument = new OfficeDocument();

		officeDocument.setTitle("" + metadata.get("Title"));
		officeDocument.setAuthor("" + metadata.get("Author"));
		officeDocument.setKeywords("" + metadata.get("Keywords"));
		officeDocument.setComments("" + metadata.get("Comments"));
		officeDocument.setCreateDateTime("" + metadata.get("CreateDateTime"));
		officeDocument.setLastSaveDateTime("" + metadata.get("LastSaveDateTime"));

		return officeDocument;
	}

	public SearchFile getFromOffice2003(InputStream is) {
		OfficeDocument eDiscoveryDocument = new OfficeDocument();

		XSSFWorkbook readMetadata = null;
		POIXMLProperties props;
		try {
			readMetadata = new XSSFWorkbook(is);
			props = readMetadata.getProperties();

		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} // Read the Excel Workbook in a instance object
		finally {
			try {
				readMetadata.close();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		POIXMLProperties.CoreProperties coreProp = props.getCoreProperties();
		/* Read and print core properties as SOP */
		System.out.println("Document Creator :" + coreProp.getCreator());
		System.out.println("Description :" + coreProp.getDescription());
		System.out.println("Keywords :" + coreProp.getKeywords());
		System.out.println("Title :" + coreProp.getTitle());
		System.out.println("Subject :" + coreProp.getSubject());
		System.out.println("Category :" + coreProp.getCategory());

		/* Read and print extended properties */
		POIXMLProperties.ExtendedProperties extProp = props.getExtendedProperties();
		System.out.println("Company :" + extProp.getUnderlyingProperties().getCompany());
		System.out.println("Template :" + extProp.getUnderlyingProperties().getTemplate());
		System.out.println("Manager Name :" + extProp.getUnderlyingProperties().getManager());

		/* Finally, we can retrieve some custom Properies */
		POIXMLProperties.CustomProperties custProp = props.getCustomProperties();
		List<CTProperty> my1 = custProp.getUnderlyingProperties().getPropertyList();
		System.out.println("Size :" + my1.size());
		for (int i = 0; i < my1.size(); i++) {
			CTProperty pItem = my1.get(i);
			System.out.println("" + pItem.getPid());
			System.out.println("" + pItem.getFmtid());
			System.out.println("" + pItem.getName());
			System.out.println("" + pItem.getLpwstr());

		}

		eDiscoveryDocument.setTitle("" + coreProp.getTitle());
		eDiscoveryDocument.setAuthor("" + coreProp.getCreator());
		eDiscoveryDocument.setKeywords("" + coreProp.getKeywords());
		eDiscoveryDocument.setComments(null);
		eDiscoveryDocument.setCreateDateTime("" + coreProp.getCreated());
		eDiscoveryDocument.setLastSaveDateTime("" + coreProp.getModified());

		return eDiscoveryDocument;
	}

}