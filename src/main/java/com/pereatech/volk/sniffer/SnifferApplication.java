package com.pereatech.volk.sniffer;

import java.io.File;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pereatech.volk.sniffer.filters.IndexedFileFilter;

@SpringBootApplication
public class SnifferApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnifferApplication.class, args);
	}

	@Bean(name = "indexedFileFilter")
	public IndexedFileFilter<File> indexedFileFilter() {
		return new IndexedFileFilter<>();
	}

	/**
	 * File-backed idempotent store so already-ingested files are skipped
	 * across restarts.
	 */
	@Bean(name = "fileIdempotentRepository")
	public IdempotentRepository fileIdempotentRepository(
			@Value("${volk.sniffer.idempotent-store}") String storePath) {
		File store = new File(storePath);
		if (store.getParentFile() != null) {
			store.getParentFile().mkdirs();
		}
		return FileIdempotentRepository.fileIdempotentRepository(store);
	}

	@Bean
	public RestClient volkRestClient(@Value("${volk.api.base-url}") String baseUrl,
			@Value("${volk.api.key:}") String apiKey, ObjectMapper objectMapper) {
		return RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeaders(headers -> {
					if (!apiKey.isBlank()) {
						headers.set("X-API-Key", apiKey);
					}
				})
				.messageConverters(converters -> {
					converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
					converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
				})
				.build();
	}
}
