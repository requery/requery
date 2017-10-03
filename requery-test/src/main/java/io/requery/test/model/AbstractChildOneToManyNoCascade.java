package io.requery.test.model;


import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.ReferentialAction;

/**
 * Created by mluchi on 03/08/2017.
 */
@Entity(cacheable = false)
public abstract class AbstractChildOneToManyNoCascade {

    @Key
    long id;

    @Column
    String attribute;

    @ManyToOne(cascade = CascadeAction.NONE)
    @ForeignKey(update = ReferentialAction.RESTRICT, delete = ReferentialAction.SET_NULL)
    ParentNoCascade parent;

}
