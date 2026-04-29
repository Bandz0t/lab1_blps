package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.Privilege;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByName(String name);
}
