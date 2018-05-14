package com.pereatech.volk.sniffer;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

import com.pereatech.volk.sniffer.filters.OfficeDocumentFilter;

@SpringBootApplication
public class CamelExchangeMainApplication{

	public static void main(String[] args) {
		SpringApplication.run(CamelExchangeMainApplication.class, args);
	}

	@Bean(name="officeDocumentFilter")
	public OfficeDocumentFilter<File> OfficeDocumentFilter() {
		return new OfficeDocumentFilter<File>();
	}
}
