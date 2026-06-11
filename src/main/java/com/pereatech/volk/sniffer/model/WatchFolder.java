package com.pereatech.volk.sniffer.model;

public record WatchFolder(
		String path,
		boolean recursive,
		String sourceId,
		String sourceName,
		String sourceType,
		String department,
		String sourceOwner) {
}
