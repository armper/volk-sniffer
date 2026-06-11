package com.pereatech.volk.sniffer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.WatchFolder;

@Service
public class DocumentProvenanceExtractor {

	public void extract(Path path, WatchFolder source, SearchFile searchFile) {
		Path sourceRoot = Path.of(source.path()).toAbsolutePath().normalize();
		Path absolutePath = path.toAbsolutePath().normalize();

		searchFile.setSourceId(source.sourceId());
		searchFile.setSourceName(source.sourceName());
		searchFile.setSourceType(source.sourceType());
		searchFile.setSourceRoot(sourceRoot.toString());
		searchFile.setRelativePath(relativePath(sourceRoot, absolutePath));
		searchFile.setDepartment(source.department());
		searchFile.setAccessContextRoot(sourceRoot.toString());
		searchFile.setSourceAccessSummary(accessSummary(sourceRoot));

		if (hasText(source.sourceOwner())) {
			searchFile.setContentOwner(source.sourceOwner());
			searchFile.setOwnershipBasis("SOURCE_PROFILE");
		} else if (hasText(searchFile.getAuthor())) {
			searchFile.setContentOwner(searchFile.getAuthor());
			searchFile.setOwnershipBasis("DOCUMENT_METADATA");
		} else {
			searchFile.setContentOwner(searchFile.getFileOwner());
			searchFile.setOwnershipBasis("FILESYSTEM_OWNER");
		}
	}

	private String relativePath(Path sourceRoot, Path path) {
		try {
			return sourceRoot.relativize(path).toString();
		} catch (IllegalArgumentException e) {
			return path.getFileName().toString();
		}
	}

	private String accessSummary(Path sourceRoot) {
		try {
			PosixFileAttributes attributes = Files.readAttributes(sourceRoot, PosixFileAttributes.class,
					LinkOption.NOFOLLOW_LINKS);
			return "owner=" + attributes.owner().getName()
					+ "; group=" + attributes.group().getName()
					+ "; permissions=" + toPermissionString(attributes.permissions());
		} catch (UnsupportedOperationException | IOException e) {
			try {
				return "owner=" + Files.getOwner(sourceRoot, LinkOption.NOFOLLOW_LINKS).getName();
			} catch (IOException ignored) {
				return "Source access context unavailable";
			}
		}
	}

	private String toPermissionString(Set<PosixFilePermission> permissions) {
		List<PosixFilePermission> order = List.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE);
		StringBuilder result = new StringBuilder(9);
		for (PosixFilePermission permission : order) {
			result.append(permissions.contains(permission) ? permissionCharacter(permission) : '-');
		}
		return result.toString();
	}

	private char permissionCharacter(PosixFilePermission permission) {
		return switch (permission) {
		case OWNER_READ, GROUP_READ, OTHERS_READ -> 'r';
		case OWNER_WRITE, GROUP_WRITE, OTHERS_WRITE -> 'w';
		case OWNER_EXECUTE, GROUP_EXECUTE, OTHERS_EXECUTE -> 'x';
		};
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
