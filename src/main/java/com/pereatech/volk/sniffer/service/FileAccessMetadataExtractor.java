package com.pereatech.volk.sniffer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.pereatech.volk.sniffer.model.SearchFile;

@Service
public class FileAccessMetadataExtractor {

	public void extract(Path path, Path watchedRoot, SearchFile searchFile) throws IOException {
		searchFile.setIndexerUser(System.getProperty("user.name"));
		searchFile.setIndexerReadable(isReadableThroughRoot(path, watchedRoot));
		searchFile.setFileOwner(Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).getName());

		boolean foundAccessMetadata = extractPosix(path, searchFile);
		if (extractAcl(path, searchFile)) {
			searchFile.setAccessControlSource(foundAccessMetadata ? "POSIX+ACL" : "ACL");
		} else if (foundAccessMetadata) {
			searchFile.setAccessControlSource("POSIX");
		} else {
			searchFile.setAccessControlSource("INDEXER");
		}
	}

	private boolean extractPosix(Path path, SearchFile searchFile) {
		try {
			PosixFileAttributes attributes = Files.readAttributes(path, PosixFileAttributes.class,
					LinkOption.NOFOLLOW_LINKS);
			Set<PosixFilePermission> permissions = attributes.permissions();
			searchFile.setFileGroup(attributes.group().getName());
			searchFile.setPosixPermissions(toPermissionString(permissions));
			searchFile.setOwnerReadable(permissions.contains(PosixFilePermission.OWNER_READ));
			searchFile.setGroupReadable(permissions.contains(PosixFilePermission.GROUP_READ));
			searchFile.setOthersReadable(permissions.contains(PosixFilePermission.OTHERS_READ));
			return true;
		} catch (UnsupportedOperationException | IOException e) {
			return false;
		}
	}

	private boolean extractAcl(Path path, SearchFile searchFile) {
		AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class,
				LinkOption.NOFOLLOW_LINKS);
		if (view == null) {
			return false;
		}

		try {
			for (AclEntry entry : view.getAcl()) {
				if (!entry.permissions().contains(AclEntryPermission.READ_DATA)) {
					continue;
				}
				String principal = entry.principal().getName();
				if (entry.type() == AclEntryType.DENY) {
					searchFile.getDeniedPrincipals().add(principal);
				} else if (entry.type() == AclEntryType.ALLOW) {
					searchFile.getAllowedPrincipals().add(principal);
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private boolean isReadableThroughRoot(Path path, Path watchedRoot) {
		Path normalizedRoot = watchedRoot.toAbsolutePath().normalize();
		Path current = path.toAbsolutePath().normalize();
		if (!Files.isReadable(current)) {
			return false;
		}
		current = current.getParent();
		while (current != null && current.startsWith(normalizedRoot)) {
			if (!Files.isExecutable(current)) {
				return false;
			}
			if (current.equals(normalizedRoot)) {
				break;
			}
			current = current.getParent();
		}
		return true;
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
			result.append(permissions.contains(permission) ? permissionCharacter(permission) : "-");
		}
		return result.toString();
	}

	private String permissionCharacter(PosixFilePermission permission) {
		return switch (permission) {
		case OWNER_READ, GROUP_READ, OTHERS_READ -> "r";
		case OWNER_WRITE, GROUP_WRITE, OTHERS_WRITE -> "w";
		case OWNER_EXECUTE, GROUP_EXECUTE, OTHERS_EXECUTE -> "x";
		};
	}
}
