package com.pereatech.volk.sniffer.model;

import java.math.BigInteger;
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
}
