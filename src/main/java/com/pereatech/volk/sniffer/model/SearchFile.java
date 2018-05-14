package com.pereatech.volk.sniffer.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@Document
@ToString
@EqualsAndHashCode(of = { "fileName", "path", "server" })
public class SearchFile {

	@Id
	protected String id;

	protected Long size;

	protected LocalDateTime createdDateTime, lastModified;

	protected String fileName, path, extension, server, share;
}
