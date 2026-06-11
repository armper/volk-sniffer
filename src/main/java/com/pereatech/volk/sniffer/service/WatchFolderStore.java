package com.pereatech.volk.sniffer.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pereatech.volk.sniffer.model.WatchFolder;

@Service
public class WatchFolderStore {

	private static final Set<String> SOURCE_TYPES = Set.of(
			"LOCAL_FOLDER", "SHARED_DRIVE", "NETWORK_SHARE", "ARCHIVE");

	private final ObjectMapper objectMapper;

	private final Path storePath;

	private final List<WatchFolder> folders;

	public WatchFolderStore(ObjectMapper objectMapper,
			@Value("${volk.sniffer.directories}") List<String> configuredDirectories,
			@Value("${volk.sniffer.recursive:false}") boolean recursive,
			@Value("${volk.sniffer.watch-folders-store}") String storePath) throws IOException {
		this.objectMapper = objectMapper;
		this.storePath = Path.of(storePath).toAbsolutePath().normalize();
		this.folders = load(configuredDirectories, recursive);
	}

	public synchronized List<WatchFolder> list() {
		return List.copyOf(folders);
	}

	public synchronized Optional<WatchFolder> find(String directory) {
		String normalized = normalize(directory);
		return folders.stream().filter(folder -> sameDirectory(folder.path(), normalized)).findFirst();
	}

	public WatchFolder validate(String directory, boolean recursive, String sourceName, String sourceType,
			String department, String sourceOwner) throws IOException {
		if (directory == null || directory.isBlank()) {
			throw new IllegalArgumentException("Choose a folder first.");
		}
		Path path = Path.of(directory).toAbsolutePath().normalize();
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("That folder does not exist.");
		}
		if (!Files.isReadable(path)) {
			throw new IllegalArgumentException("Volk cannot read that folder.");
		}
		Path realPath = path.toRealPath();
		return profile(realPath.toString(), recursive, sourceName, sourceType, department, sourceOwner);
	}

	public String normalize(String directory) {
		if (directory == null || directory.isBlank()) {
			throw new IllegalArgumentException("Folder path is required.");
		}
		return Path.of(directory).toAbsolutePath().normalize().toString();
	}

	public synchronized void put(WatchFolder folder) throws IOException {
		folders.removeIf(existing -> existing.path().equals(folder.path()));
		folders.add(folder);
		folders.sort((left, right) -> left.path().compareToIgnoreCase(right.path()));
		persist();
	}

	public synchronized void remove(String directory) throws IOException {
		String normalized = normalize(directory);
		folders.removeIf(folder -> sameDirectory(folder.path(), normalized));
		persist();
	}

	private boolean sameDirectory(String left, String right) {
		if (left.equals(right)) {
			return true;
		}
		try {
			return Files.isSameFile(Path.of(left), Path.of(right));
		} catch (IOException e) {
			return false;
		}
	}

	private List<WatchFolder> load(List<String> configuredDirectories, boolean recursive) throws IOException {
		if (Files.isRegularFile(storePath)) {
			List<WatchFolder> saved = objectMapper.readValue(storePath.toFile(), new TypeReference<>() { });
			List<WatchFolder> normalized = new ArrayList<>();
			for (WatchFolder folder : saved) {
				normalized.add(profile(folder.path(), folder.recursive(), folder.sourceName(), folder.sourceType(),
						folder.department(), folder.sourceOwner()));
			}
			return normalized;
		}

		List<WatchFolder> defaults = new ArrayList<>();
		for (String directory : configuredDirectories) {
			if (directory != null && !directory.isBlank()) {
				defaults.add(profile(normalize(directory), recursive, null, null, null, null));
			}
		}
		return defaults;
	}

	private WatchFolder profile(String path, boolean recursive, String sourceName, String sourceType,
			String department, String sourceOwner) {
		String normalizedPath = normalize(path);
		Path profilePath = Path.of(normalizedPath);
		String fallbackName = profilePath.getFileName() == null ? normalizedPath : profilePath.getFileName().toString();
		String normalizedName = valueOrDefault(sourceName, fallbackName);
		String normalizedType = valueOrDefault(sourceType, inferSourceType(normalizedPath)).toUpperCase(Locale.ROOT);
		if (!SOURCE_TYPES.contains(normalizedType)) {
			throw new IllegalArgumentException("Choose a valid source type.");
		}
		validateLength(normalizedName, "Source name");
		validateLength(department, "Department or team");
		validateLength(sourceOwner, "Responsible owner");
		String sourceId = UUID.nameUUIDFromBytes(normalizedPath.getBytes(StandardCharsets.UTF_8)).toString();
		return new WatchFolder(normalizedPath, recursive, sourceId, normalizedName, normalizedType,
				clean(department), clean(sourceOwner));
	}

	private String inferSourceType(String path) {
		return path.startsWith("/Volumes/") ? "SHARED_DRIVE" : "LOCAL_FOLDER";
	}

	private String valueOrDefault(String value, String fallback) {
		String cleaned = clean(value);
		return cleaned.isBlank() ? fallback : cleaned;
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}

	private void validateLength(String value, String label) {
		if (value != null && value.trim().length() > 120) {
			throw new IllegalArgumentException(label + " must be 120 characters or fewer.");
		}
	}

	private void persist() throws IOException {
		Path parent = storePath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Path temporary = storePath.resolveSibling(storePath.getFileName() + ".tmp");
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), folders);
		try {
			Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temporary, storePath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
