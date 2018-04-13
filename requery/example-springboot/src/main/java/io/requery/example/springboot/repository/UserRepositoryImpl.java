package io.requery.example.springboot.repository;

import io.requery.example.springboot.entity.User;
import io.requery.example.springboot.entity.UserEntity;
import io.requery.sql.EntityDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    @Autowired
    EntityDataStore dataStore;

    @Override
    public User findById(int id) {
        return (User) dataStore.findByKey(User.class, id);
    }

    @Override
    public User save(User user) {
        return (User) dataStore.upsert(user);
    }

    @Override
    public void delete(User user) {
        dataStore.delete(user);
    }

    @Override
    public void delete(int id) {
        dataStore.delete(UserEntity.class).where(UserEntity.ID.eq(id));
    }
}
