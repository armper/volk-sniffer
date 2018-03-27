package com.pereatech.volk.sniffer.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.SearchUser;

import lombok.extern.log4j.Log4j2;

@Service
public class SearchUserRestRepository {
	private RestTemplate restTemplate = new RestTemplate();
	private URI resourceUrl = URI.create("http://localhost:8091/searchuser");

	public SearchUser save(SearchUser searchUser) {
		HttpEntity<SearchUser> httpEntity = new HttpEntity<>(searchUser);
		return restTemplate.postForObject(resourceUrl, httpEntity, SearchUser.class);
	}
}
