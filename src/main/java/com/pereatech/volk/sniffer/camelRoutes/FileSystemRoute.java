package com.pereatech.volk.sniffer.camelRoutes;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.SearchUser;
import com.pereatech.volk.sniffer.repository.SearchFileRepository;
import com.pereatech.volk.sniffer.repository.SearchUserRepository;
import com.pereatech.volk.sniffer.service.MsOfficeExtractor;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class FileSystemRoute extends RouteBuilder {
	private final SearchUserRepository searchUserRepository;

	@Autowired
	public FileSystemRoute(SearchUserRepository searchUserRepository) {
		super();
		this.searchUserRepository = searchUserRepository;
	}

	@Override
	public void configure() throws Exception {

		List<String> servers = new ArrayList<>();
		servers.add("\\\\KIVA10442\\Temp\\");

		servers.stream().forEach(server -> {

			String routeId = UUID.randomUUID().toString();

			String arguments = "?noop=true";

			from("file://" + server + arguments).routeId("FileRoute" + routeId).process(new Processor() {

				public void process(Exchange exchange) throws Exception {

					GenericFile<File> genericFile = (GenericFile<File>) exchange.getIn().getBody();

					String absolutePath = genericFile.getFile().getAbsolutePath();

					log.debug("Processing path: " + absolutePath);

					Path path = Paths.get(absolutePath);

					String fileName = path.getFileName().toString();
					log.debug("File name is " + fileName);

					String extension = FilenameUtils.getExtension(fileName);
					log.debug("File extension is " + extension);

					SearchUser createdBy = new SearchUser();
					createdBy.setDomainName(StringUtils.substringBefore(Files.getOwner(path).getName(), "\\"));

					createdBy.setName(StringUtils.substringAfter(Files.getOwner(path).getName(), "\\"));

					SearchUser findOneByNameAndDomainName = searchUserRepository
							.findOneByNameAndDomainName(createdBy.getName(), createdBy.getDomainName());
					if (findOneByNameAndDomainName == null)
						createdBy = searchUserRepository.save(createdBy);
					else
						createdBy = findOneByNameAndDomainName;

					log.debug(createdBy);

					BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);

					LocalDateTime lastModified = LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(),
							ZoneOffset.UTC);
					LocalDateTime lastAccessed = LocalDateTime.ofInstant(attributes.lastAccessTime().toInstant(),
							ZoneOffset.UTC);

					LocalDateTime creationTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
							ZoneOffset.UTC);

					Long length = attributes.size();

					SearchFile searchFile = null;

					MsOfficeExtractor msOfficeExtractor = new MsOfficeExtractor();

					// get byte array of any MS office document
					InputStream is = Files.newInputStream(path);

					Collection<String> office97Types = Arrays.asList("doc", "xls", "ppt");
					Collection<String> office2003Types = Arrays.asList("docx", "xlsx", "pptx");

					if (office97Types.contains(extension)) {
						searchFile = msOfficeExtractor.getFromOffice97(is);
					} else if (extension.equals("xlsx")) {
						searchFile = msOfficeExtractor.getFromOffice2003(is);
					}

					if (searchFile == null)
						searchFile = new SearchFile();

					searchFile.setFileName(fileName);
					searchFile.setExtension(extension);
					searchFile.setPath(absolutePath);
					searchFile.setServer(server);
					searchFile.setLastModified(lastModified);
					searchFile.setSize(length);
					searchFile.setCreatedDateTime(creationTime);

					createdBy.getSearchFiles().add(searchFile);
					
					createdBy = searchUserRepository.save(createdBy);

					log.debug(searchUserRepository.findOneById(createdBy.getId()));

				}
			});
		});
	}
}
