package com.pereatech.volk.sniffer.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SearchFile {

	protected String id;
	
	protected Long size;
	
	protected LocalDateTime createdDateTime, lastModified;

	protected String fileName, path, extension, server, share;
}
