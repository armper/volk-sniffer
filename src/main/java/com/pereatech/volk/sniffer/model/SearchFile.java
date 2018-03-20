package com.pereatech.volk.sniffer.model;

import java.time.LocalDateTime;
import java.util.UUID;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SearchFile {

	protected Object id;
	
	protected SearchUser createdBy;

	protected Long size;
	
	protected LocalDateTime createdDateTime, lastModified;

	protected String fileName, path, extension, server, share;
}
