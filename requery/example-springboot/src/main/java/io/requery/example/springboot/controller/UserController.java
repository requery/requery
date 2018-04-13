package io.requery.example.springboot.controller;

import io.requery.example.springboot.entity.User;
import io.requery.example.springboot.entity.UserEntity;
import io.requery.example.springboot.repository.UserRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController("/user")
public class UserController {

    @Autowired
    UserRepositoryImpl userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody UserEntity user) {
        return userRepository.save(user);
    }

    @GetMapping
    public User get(@RequestParam("id") int id) {
        return userRepository.findById(id);
    }
}
