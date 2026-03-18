package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserDataService {

    User create(User user);

    User getById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    User save(User user);
}
