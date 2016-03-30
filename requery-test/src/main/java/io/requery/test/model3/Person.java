package io.requery.test.model3;


import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Index;
import io.requery.Key;
import io.requery.Lazy;
import io.requery.Naming;
import io.requery.Nullable;

import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Entity(model = "model3")
public final class Person implements Serializable {

    @Key @Generated
    private int id;

    private String name;
    @Index(value = "email_index")
    private String email;
    private Date birthday;
    @Nullable
    private int age;

    @Lazy
    private String about;

    @Column(unique = true)
    @Naming(getter = "getUUID", setter = "setUUID")
    private UUID uuid;
    private URL homepage;

    private String picture;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public URL getHomepage() {
        return homepage;
    }

    public void setHomepage(URL homepage) {
        this.homepage = homepage;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
