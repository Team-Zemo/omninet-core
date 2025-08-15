package org.zemo.omninet.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.chat.model.Contact;
import org.zemo.omninet.security.model.User;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, String> {

    List<Contact> findByOwner(User owner);
    boolean existsByOwnerAndContact(User owner, User contact);
    Contact findByOwnerAndContact(User owner, User contact);
}
