package com.pereatech.volk.sniffer.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode(of = { "name", "domainName" })
public class SearchUser {

	private String id;

	private String name, domainName;

	public SearchUser(String name, String domainName) {
		this.name = name;
		this.domainName = domainName;
	}
}
