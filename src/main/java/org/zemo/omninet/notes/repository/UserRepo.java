package org.zemo.omninet.notes.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.zemo.omninet.security.model.User;

public interface UserRepo extends JpaRepository<User, Integer> {
    Boolean existsByEmail(String email);

    User findByEmail(String username);
}
