package com.pereatech.volk.sniffer.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pereatech.volk.sniffer.camelRoutes.FileSystemRoute;
import com.pereatech.volk.sniffer.model.WatchFolder;
import com.pereatech.volk.sniffer.service.WatchFolderStore;

@RestController
@RequestMapping("/api/watch-folders")
@CrossOrigin(origins = "${volk.cors.allowed-origins:http://localhost:4200}")
public class WatchFolderController {

	private final FileSystemRoute fileSystemRoute;

	private final WatchFolderStore watchFolderStore;

	public WatchFolderController(FileSystemRoute fileSystemRoute, WatchFolderStore watchFolderStore) {
		this.fileSystemRoute = fileSystemRoute;
		this.watchFolderStore = watchFolderStore;
	}

	@GetMapping
	public List<WatchFolderStatus> list() {
		return watchFolderStore.list().stream()
				.map(folder -> new WatchFolderStatus(folder.path(), folder.recursive(),
						fileSystemRoute.routeStatus(folder.path())))
				.toList();
	}

	@PostMapping
	public WatchFolderStatus add(@RequestBody WatchFolderRequest request) {
		try {
			WatchFolder folder = fileSystemRoute.addFolder(request.path(), request.recursive());
			return new WatchFolderStatus(folder.path(), folder.recursive(), fileSystemRoute.routeStatus(folder.path()));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start watching that folder.", e);
		}
	}

	@DeleteMapping
	public void remove(@RequestParam String path) {
		try {
			if (!fileSystemRoute.removeFolder(path)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That folder is not being watched.");
			}
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not stop watching that folder.", e);
		}
	}

	@GetMapping("/browse")
	public DirectoryListing browse(@RequestParam(required = false) String path) {
		Path directory = path == null || path.isBlank()
				? Path.of(System.getProperty("user.home"))
				: Path.of(path);
		directory = directory.toAbsolutePath().normalize();

		if (!Files.isDirectory(directory) || !Files.isReadable(directory)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That folder is not available.");
		}

		try (var children = Files.list(directory)) {
			List<DirectoryEntry> directories = children
					.filter(Files::isDirectory)
					.filter(Files::isReadable)
					.filter(child -> !isHidden(child))
					.sorted(Comparator.comparing(child -> child.getFileName().toString().toLowerCase()))
					.limit(250)
					.map(child -> new DirectoryEntry(child.getFileName().toString(), child.toString()))
					.toList();
			Path parent = directory.getParent();
			return new DirectoryListing(directory.toString(), parent == null ? null : parent.toString(),
					System.getProperty("user.home"), directories);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Volk cannot browse that folder.", e);
		}
	}

	private boolean isHidden(Path path) {
		try {
			return Files.isHidden(path);
		} catch (IOException e) {
			return true;
		}
	}

	public record WatchFolderRequest(String path, boolean recursive) { }

	public record WatchFolderStatus(String path, boolean recursive, String status) { }

	public record DirectoryEntry(String name, String path) { }

	public record DirectoryListing(String path, String parent, String home, List<DirectoryEntry> directories) { }
}
