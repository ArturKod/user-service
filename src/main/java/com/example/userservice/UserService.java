package com.example.userservice;

import com.example.userservice.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(String name, String email, Integer age);

    Optional<User> getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(Long id, String name, String email, Integer age);

    void deleteUser(Long id);
}