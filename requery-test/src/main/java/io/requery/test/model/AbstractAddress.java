package io.requery.test.model;


import io.requery.Column;
import io.requery.Convert;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import io.requery.OneToOne;

@Entity
public class AbstractAddress extends Coordinate {

    @Key @Generated
    protected int id;

    protected String line1;
    protected String line2;

    protected String state;

    @Column(length = 5)
    protected String zip;

    @Column(length = 2)
    protected String country;

    protected String city;

    @OneToOne(mappedBy = "address")
    protected Person person;

    @Convert(AddressTypeConverter.class)
    protected AddressType type;

    @Override
    public String toString() {
        return "Don't override me";
    }
}
