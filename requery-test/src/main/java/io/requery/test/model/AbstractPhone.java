package io.requery.test.model;

import io.requery.Entity;
import io.requery.Factory;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;

import java.io.Serializable;

@Entity
@Factory(PhoneFactory.class)
public class AbstractPhone implements Serializable {

    public AbstractPhone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AbstractPhone(String phoneNumber, boolean normalized) {
        this.phoneNumber = phoneNumber;
        this.normalized = normalized;
    }

    public AbstractPhone() {
    }

    @Key @Generated
    protected int id;
    protected String phoneNumber;
    protected boolean normalized;

    @ManyToOne
    protected Person owner;
}
