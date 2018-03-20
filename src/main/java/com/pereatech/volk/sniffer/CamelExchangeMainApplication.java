package com.pereatech.volk.sniffer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.pereatech.volk.sniffer.filters.OfficeDocumentFilter;

import jcifs.smb.SmbFile;

@SpringBootApplication
public class CamelExchangeMainApplication{

	public static void main(String[] args) {
		SpringApplication.run(CamelExchangeMainApplication.class, args);
	}

	@Bean(name="officeDocumentFilter")
	public OfficeDocumentFilter<SmbFile> OfficeDocumentFilter() {
		return new OfficeDocumentFilter<SmbFile>();
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
}
