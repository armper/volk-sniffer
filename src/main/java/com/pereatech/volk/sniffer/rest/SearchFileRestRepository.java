package com.pereatech.volk.sniffer.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.pereatech.volk.sniffer.model.SearchFile;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class SearchFileRestRepository {
	private RestTemplate restTemplate = new RestTemplate();
	private URI fooResourceUrl = URI.create("http://localhost:8091/searchfile");

	public SearchFile save(SearchFile searchFile) {
		log.debug(searchFile);
		HttpEntity<SearchFile> httpEntity = new HttpEntity<>(searchFile);
		return restTemplate.postForObject(fooResourceUrl, httpEntity, SearchFile.class);
	}


}
