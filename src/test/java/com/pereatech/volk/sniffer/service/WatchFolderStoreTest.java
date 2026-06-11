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
		first.put(first.validate(added.toString(), true, "Case files", "SHARED_DRIVE", "Legal", "Records team"));
		first.remove(watched.toString());

		WatchFolderStore reloaded = new WatchFolderStore(new ObjectMapper(), List.of(), false, store.toString());

		WatchFolder folder = reloaded.list().get(0);
		assertThat(folder.path()).isEqualTo(added.toRealPath().toString());
		assertThat(folder.sourceName()).isEqualTo("Case files");
		assertThat(folder.sourceType()).isEqualTo("SHARED_DRIVE");
		assertThat(folder.department()).isEqualTo("Legal");
		assertThat(folder.sourceOwner()).isEqualTo("Records team");
		assertThat(folder.sourceId()).isNotBlank();
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

	@Test
	void migratesLegacyFolderRecordsWithUsefulDefaults() throws Exception {
		Path legacyFolder = temporaryDirectory.resolve("legacy");
		java.nio.file.Files.createDirectories(legacyFolder);
		Path storeFile = temporaryDirectory.resolve("legacy-folders.json");
		java.nio.file.Files.writeString(storeFile,
				"[{\"path\":\"" + legacyFolder + "\",\"recursive\":true}]");

		WatchFolderStore store = new WatchFolderStore(new ObjectMapper(), List.of(), false, storeFile.toString());
		WatchFolder migrated = store.list().get(0);

		assertThat(migrated.sourceName()).isEqualTo("legacy");
		assertThat(migrated.sourceType()).isEqualTo("LOCAL_FOLDER");
		assertThat(migrated.sourceId()).isNotBlank();
		assertThat(migrated.department()).isEmpty();
	}
}
