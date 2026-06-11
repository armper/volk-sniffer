package com.pereatech.volk.sniffer;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.pereatech.volk.sniffer.filters.OfficeDocumentFilter;

@SpringBootApplication
public class SnifferApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnifferApplication.class, args);
	}

	@Bean(name = "officeDocumentFilter")
	public OfficeDocumentFilter<File> officeDocumentFilter() {
		return new OfficeDocumentFilter<>();
	}
}
