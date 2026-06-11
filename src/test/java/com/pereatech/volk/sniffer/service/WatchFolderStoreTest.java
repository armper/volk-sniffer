package com.pereatech.volk.sniffer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pereatech.volk.sniffer.model.WatchFolder;

class WatchFolderStoreTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void persistsFolderChangesAcrossInstances() throws Exception {
		Path watched = temporaryDirectory.resolve("watched");
		Path added = temporaryDirectory.resolve("added");
		java.nio.file.Files.createDirectories(watched);
		java.nio.file.Files.createDirectories(added);
		Path store = temporaryDirectory.resolve("watch-folders.json");

		WatchFolderStore first = new WatchFolderStore(new ObjectMapper(), List.of(watched.toString()), false,
				store.toString());
		first.put(first.validate(added.toString(), true));
		first.remove(watched.toString());

		WatchFolderStore reloaded = new WatchFolderStore(new ObjectMapper(), List.of(), false, store.toString());

		assertThat(reloaded.list()).containsExactly(new WatchFolder(added.toRealPath().toString(), true));
	}

	@Test
	void recognizesDifferentPathsToTheSameFolder() throws Exception {
		Path realFolder = temporaryDirectory.resolve("real");
		Path alias = temporaryDirectory.resolve("alias");
		java.nio.file.Files.createDirectories(realFolder);
		java.nio.file.Files.createSymbolicLink(alias, realFolder);

		WatchFolderStore store = new WatchFolderStore(new ObjectMapper(), List.of(alias.toString()), false,
				temporaryDirectory.resolve("aliases.json").toString());

		assertThat(store.find(realFolder.toString())).isPresent();
	}
}
