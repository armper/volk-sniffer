package com.pereatech.volk.sniffer.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SearchUser {

	protected String id;
	
	private String name, domainName;
	
	private List<SearchFile> searchFiles = new ArrayList<>();

}
