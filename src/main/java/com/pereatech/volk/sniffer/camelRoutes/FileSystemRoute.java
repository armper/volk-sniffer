package com.pereatech.volk.sniffer.camelRoutes;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.SearchUser;
import com.pereatech.volk.sniffer.repository.SearchUserRepository;
import com.pereatech.volk.sniffer.service.MsOfficeExtractor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileSystemRoute extends RouteBuilder {

	private static final Set<String> OLE2_TYPES = Set.of("doc", "xls", "ppt");

	private static final Set<String> OOXML_TYPES = Set.of("docx", "docm", "xlsx", "xlsm", "pptx", "pptm", "potm");

	private final SearchUserRepository searchUserRepository;

	private final MsOfficeExtractor msOfficeExtractor = new MsOfficeExtractor();

	@Value("${volk.sniffer.directories}")
	private List<String> directories;

	@Value("${volk.sniffer.recursive:false}")
	private boolean recursive;

	public FileSystemRoute(SearchUserRepository searchUserRepository) {
		this.searchUserRepository = searchUserRepository;
	}

	@Override
	public void configure() {
		directories.forEach(directory -> from(
				"file:" + directory + "?noop=true&recursive=" + recursive + "&filter=#officeDocumentFilter")
				.routeId("FileRoute[" + directory + "]")
				.process(exchange -> ingest(exchange, directory)));
	}

	@SuppressWarnings("unchecked")
	private void ingest(Exchange exchange, String directory) throws Exception {
		GenericFile<File> genericFile = (GenericFile<File>) exchange.getIn().getBody();
		Path path = genericFile.getFile().toPath();

		log.debug("Processing path: {}", path);

		String fileName = path.getFileName().toString();
		String extension = FilenameUtils.getExtension(fileName).toLowerCase();

		SearchUser createdBy = resolveOwner(path);

		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);

		SearchFile searchFile = extractMetadata(path, extension);

		searchFile.setFileName(fileName);
		searchFile.setExtension(extension);
		searchFile.setPath(path.toAbsolutePath().toString());
		searchFile.setServer(directory);
		searchFile.setSize(attributes.size());
		searchFile.setCreatedDateTime(LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneOffset.UTC));
		searchFile.setLastModified(LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));

		createdBy.getSearchFiles().add(searchFile);
		createdBy = searchUserRepository.save(createdBy);

		log.debug("Saved file {} for user {}", fileName, createdBy.getName());
	}

	/**
	 * Looks up (or creates) the SearchUser owning the file. Windows owners come
	 * back as DOMAIN\name; POSIX owners have no domain part.
	 */
	private SearchUser resolveOwner(Path path) throws java.io.IOException {
		String owner = Files.getOwner(path).getName();

		String domainName = StringUtils.contains(owner, "\\") ? StringUtils.substringBefore(owner, "\\") : "";
		String name = StringUtils.contains(owner, "\\") ? StringUtils.substringAfter(owner, "\\") : owner;

		SearchUser existing = searchUserRepository.findOneByNameAndDomainName(name, domainName);
		if (existing != null) {
			return existing;
		}

		SearchUser createdBy = new SearchUser();
		createdBy.setName(name);
		createdBy.setDomainName(domainName);
		return searchUserRepository.save(createdBy);
	}

	private SearchFile extractMetadata(Path path, String extension) {
		try (InputStream is = Files.newInputStream(path)) {
			if (OLE2_TYPES.contains(extension)) {
				return msOfficeExtractor.getFromOle2(is);
			}
			if (OOXML_TYPES.contains(extension)) {
				return msOfficeExtractor.getFromOoxml(is);
			}
		} catch (Exception e) {
			log.warn("Could not extract Office metadata from {}: {}", path, e.getMessage());
		}
		return new SearchFile();
	}
}
