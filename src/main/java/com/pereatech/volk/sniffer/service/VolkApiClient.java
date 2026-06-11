package com.pereatech.volk.sniffer.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.pereatech.volk.sniffer.model.SearchFile;
import com.pereatech.volk.sniffer.model.SearchUser;

/**
 * Thin client for the volk-rest API — the single writer to MongoDB.
 */
@Service
public class VolkApiClient {

	private final RestClient restClient;

	public VolkApiClient(RestClient volkRestClient) {
		this.restClient = volkRestClient;
	}

	/**
	 * Returns the persisted user (existing or newly created) including its id.
	 */
	public SearchUser getOrCreateUser(SearchUser user) {
		return restClient.post().uri("/searchuser")
				.contentType(MediaType.APPLICATION_JSON)
				.body(user)
				.retrieve()
				.body(SearchUser.class);
	}

	public SearchFile saveFile(SearchFile file) {
		return restClient.post().uri("/searchfile")
				.contentType(MediaType.APPLICATION_JSON)
				.body(file)
				.retrieve()
				.body(SearchFile.class);
	}
}
