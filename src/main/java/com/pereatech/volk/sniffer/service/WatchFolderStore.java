package com.pereatech.volk.sniffer.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pereatech.volk.sniffer.model.WatchFolder;

@Service
public class WatchFolderStore {

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

	public WatchFolder validate(String directory, boolean recursive) throws IOException {
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
		return new WatchFolder(path.toRealPath().toString(), recursive);
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
			return new ArrayList<>(objectMapper.readValue(storePath.toFile(), new TypeReference<>() { }));
		}

		List<WatchFolder> defaults = new ArrayList<>();
		for (String directory : configuredDirectories) {
			if (directory != null && !directory.isBlank()) {
				defaults.add(new WatchFolder(normalize(directory), recursive));
			}
		}
		return defaults;
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
