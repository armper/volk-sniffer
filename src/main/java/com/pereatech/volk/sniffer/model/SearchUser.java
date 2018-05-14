package com.pereatech.volk.sniffer.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@RequiredArgsConstructor
@Document
@ToString
@EqualsAndHashCode(of = { "name", "domainName" })
public class SearchUser {
	@Id
	protected String id;

	private String name, domainName;

	private List<SearchFile> searchFiles = new ArrayList<>();

}
