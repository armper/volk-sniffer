package com.pereatech.volk.sniffer.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper=true)
public class OfficeDocument extends SearchFile {

	protected String title, author, keywords, comments, createDateTime, lastSaveDateTime;

}
