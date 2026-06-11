package com.pereatech.volk.sniffer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(exclude = "content")
@EqualsAndHashCode(of = { "fileName", "path", "server" })
public class SearchFile {

	private String id;

	/** Owning SearchUser, assigned by volk-rest. */
	private String userId;

	private Long size;

	private LocalDateTime createdDateTime, lastModified;

	private String fileName, path, extension, server, share;

	/** Document metadata extracted by Tika. */
	private String title, author, keywords, comments, contentType;

	/** Human and system provenance inherited from the watched source. */
	private String sourceId, sourceName, sourceType, sourceRoot, relativePath;

	private String contentOwner, ownershipBasis, department, accessContextRoot, sourceAccessSummary;

	/** Filesystem access metadata used by volk-rest to filter results. */
	private String fileOwner, fileGroup, posixPermissions, accessControlSource, indexerUser;

	private boolean ownerReadable, groupReadable, othersReadable, indexerReadable;

	private List<String> allowedPrincipals = new ArrayList<>();

	private List<String> deniedPrincipals = new ArrayList<>();

	/** Extracted text content (capped), powers full-text search. */
	private String content;
}
