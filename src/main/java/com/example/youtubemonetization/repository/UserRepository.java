package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.privileges"})
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
