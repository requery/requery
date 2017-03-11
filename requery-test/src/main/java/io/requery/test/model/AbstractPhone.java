package io.requery.test.model;

import io.requery.Column;
import io.requery.Convert;
import io.requery.Entity;
import io.requery.Factory;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;

import java.io.Serializable;
import java.util.ArrayList;

@Entity
@Factory(PhoneFactory.class)
public class AbstractPhone implements Serializable {

    AbstractPhone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    AbstractPhone(String phoneNumber, boolean normalized) {
        this.phoneNumber = phoneNumber;
        this.normalized = normalized;
    }

    AbstractPhone() {
    }

    @Key @Generated
    protected int id;
    protected String phoneNumber;
    protected boolean normalized;

    @Column
    @Convert(IntegerListConverter.class)
    protected ArrayList<Integer> extensions = new ArrayList<>();

    @ManyToOne
    protected Person owner;
}
