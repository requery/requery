package io.requery.test.model;


import io.requery.Column;
import io.requery.Entity;
import io.requery.Key;

/**
 * Created by mluchi on 03/08/2017.
 */
@Entity(cacheable = false)
public abstract class AbstractChildManyToOneNoCascade {

    @Key
    long id;

    @Column
    String attribute;

}
