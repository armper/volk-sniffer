package com.pereatech.volk.sniffer.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.pereatech.volk.sniffer.model.SearchUser;

public interface SearchUserRepository extends PagingAndSortingRepository<SearchUser, String> {

	SearchUser findOneById(String id);

	SearchUser findOneByNameAndDomainName(String name, String domainName);
}
