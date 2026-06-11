package com.pereatech.volk.sniffer.repository;

import org.springframework.data.repository.CrudRepository;

import com.pereatech.volk.sniffer.model.SearchUser;

public interface SearchUserRepository extends CrudRepository<SearchUser, String> {

	SearchUser findOneById(String id);

	SearchUser findOneByNameAndDomainName(String name, String domainName);
}
