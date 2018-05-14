package com.pereatech.volk.sniffer.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Document
@Data
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
public class OfficeDocument extends SearchFile {

	protected String title, author, keywords, comments, createDateTime, lastSaveDateTime;

}
