package io.requery.test.model;

import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.ReferentialAction;

/**
 * Created by mluchi on 04/08/2017.
 */

@Entity
public abstract class AbstractParent {

    @Key
    @Generated
    long id;

    @ManyToOne(cascade = CascadeAction.DELETE)
    @ForeignKey(delete = ReferentialAction.SET_NULL, update = ReferentialAction.RESTRICT)
    AbstractChild child;

}
