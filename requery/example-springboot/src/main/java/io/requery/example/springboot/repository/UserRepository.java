package io.requery.example.springboot.repository;

import io.requery.example.springboot.entity.User;

public interface UserRepository {
    User findById(int id);

    User save(User user);

    void delete(User user);

    void delete(int id);
}
