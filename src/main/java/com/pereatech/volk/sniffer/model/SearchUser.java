package com.pereatech.volk.sniffer.model;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SearchUser {

	private Object id;
	
	private String name, domainName;
}
