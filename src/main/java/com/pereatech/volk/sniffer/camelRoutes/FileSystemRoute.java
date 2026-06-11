package com.pereatech.volk.sniffer.camelRoutes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.SearchUser;
import com.pereatech.volk.sniffer.model.WatchFolder;
import com.pereatech.volk.sniffer.service.TikaMetadataExtractor;
import com.pereatech.volk.sniffer.service.FileAccessMetadataExtractor;
import com.pereatech.volk.sniffer.service.DocumentProvenanceExtractor;
import com.pereatech.volk.sniffer.service.VolkApiClient;
import com.pereatech.volk.sniffer.service.WatchFolderStore;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileSystemRoute extends RouteBuilder {

	private final VolkApiClient volkApiClient;

	private final TikaMetadataExtractor metadataExtractor;

	private final FileAccessMetadataExtractor accessMetadataExtractor;

	private final DocumentProvenanceExtractor provenanceExtractor;

	private final WatchFolderStore watchFolderStore;

	public FileSystemRoute(VolkApiClient volkApiClient, TikaMetadataExtractor metadataExtractor,
			FileAccessMetadataExtractor accessMetadataExtractor, DocumentProvenanceExtractor provenanceExtractor,
			WatchFolderStore watchFolderStore) {
		this.volkApiClient = volkApiClient;
		this.metadataExtractor = metadataExtractor;
		this.accessMetadataExtractor = accessMetadataExtractor;
		this.provenanceExtractor = provenanceExtractor;
		this.watchFolderStore = watchFolderStore;
	}

	@Override
	public void configure() {
		watchFolderStore.list().forEach(folder -> from(routeUri(folder))
				.routeId(routeId(folder.path()))
				.process(exchange -> ingest(exchange, folder)));
	}

	public synchronized WatchFolder addFolder(String directory, boolean recursive, String sourceName, String sourceType,
			String department, String sourceOwner) throws Exception {
		WatchFolder folder = watchFolderStore.validate(directory, recursive, sourceName, sourceType, department,
				sourceOwner);
		WatchFolder existing = watchFolderStore.find(folder.path()).orElse(null);

		if (existing != null) {
			if (existing.equals(folder)) {
				return existing;
			}
			removeRoute(existing.path());
		}

		getContext().addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from(routeUri(folder))
						.routeId(routeId(folder.path()))
						.process(exchange -> ingest(exchange, folder));
			}
		});
		watchFolderStore.put(folder);
		return folder;
	}

	public synchronized boolean removeFolder(String directory) throws Exception {
		WatchFolder existing = watchFolderStore.find(directory).orElse(null);
		if (existing == null) {
			return false;
		}
		removeRoute(existing.path());
		watchFolderStore.remove(existing.path());
		return true;
	}

	private void removeRoute(String directory) throws Exception {
		String routeId = routeId(directory);
		if (getContext().getRoute(routeId) != null) {
			getContext().getRouteController().stopRoute(routeId);
			getContext().removeRoute(routeId);
		}
	}

	public String routeStatus(String directory) {
		var status = getContext().getRouteController().getRouteStatus(routeId(directory));
		return status == null ? "Stopped" : status.name();
	}

	private String routeUri(WatchFolder folder) {
		String pathUri = Path.of(folder.path()).toUri().toASCIIString().substring("file:".length());
		String profileVersion = Integer.toUnsignedString(folder.hashCode(), 16);
		return "file:" + pathUri
				+ "?noop=true&recursive=" + folder.recursive()
				+ "&filter=#indexedFileFilter"
				+ "&idempotent=true&idempotentRepository=#fileIdempotentRepository"
				+ "&idempotentKey=provenance-v1-" + profileVersion
				+ "-${file:absolute.path}-${file:modified}";
	}

	private String routeId(String directory) {
		UUID id = UUID.nameUUIDFromBytes(directory.getBytes(StandardCharsets.UTF_8));
		return "FileRoute-" + id;
	}

	@SuppressWarnings("unchecked")
	private void ingest(Exchange exchange, WatchFolder source) throws IOException {
		GenericFile<File> genericFile = (GenericFile<File>) exchange.getIn().getBody();
		Path path = genericFile.getFile().toPath();

		log.debug("Processing path: {}", path);

		String fileName = path.getFileName().toString();

		SearchUser owner = volkApiClient.getOrCreateUser(resolveOwner(path));

		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);

		SearchFile searchFile = new SearchFile();
		extractMetadata(path, searchFile);
		accessMetadataExtractor.extract(path, Path.of(source.path()), searchFile);
		provenanceExtractor.extract(path, source, searchFile);

		searchFile.setUserId(owner.getId());
		searchFile.setFileName(fileName);
		searchFile.setExtension(FilenameUtils.getExtension(fileName).toLowerCase());
		searchFile.setPath(path.toAbsolutePath().toString());
		searchFile.setServer(source.path());
		searchFile.setSize(attributes.size());
		searchFile.setCreatedDateTime(LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneOffset.UTC));
		searchFile.setLastModified(LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));

		SearchFile saved = volkApiClient.saveFile(searchFile);

		log.debug("Ingested {} as {} for user {}", fileName, saved.getId(), owner.getName());
	}

	/**
	 * Windows owners come back as DOMAIN\name; POSIX owners have no domain part.
	 */
	private SearchUser resolveOwner(Path path) throws IOException {
		String owner = Files.getOwner(path).getName();

		String domainName = StringUtils.contains(owner, "\\") ? StringUtils.substringBefore(owner, "\\") : "";
		String name = StringUtils.contains(owner, "\\") ? StringUtils.substringAfter(owner, "\\") : owner;

		return new SearchUser(name, domainName);
	}

	private void extractMetadata(Path path, SearchFile searchFile) {
		try (InputStream is = Files.newInputStream(path)) {
			metadataExtractor.extract(is, searchFile);
		} catch (Exception e) {
			log.warn("Could not extract metadata from {}: {}", path, e.getMessage());
		}
	}
}
