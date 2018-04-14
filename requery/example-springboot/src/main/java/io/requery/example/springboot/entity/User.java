package io.requery.example.springboot.entity;

import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.Table;

@Entity
@Table(name = "example_user")
public class User {
    @Key
    int id;
    String firstName;
    String lastName;

    public User() {
    }

    public User(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
