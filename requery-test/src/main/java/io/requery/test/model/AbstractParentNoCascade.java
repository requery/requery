package io.requery.test.model;


import java.util.List;

import io.requery.CascadeAction;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.JunctionTable;
import io.requery.Key;
import io.requery.ManyToMany;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.OneToOne;
import io.requery.ReferentialAction;


/**
 * Created by mluchi on 03/08/2017.
 */

@Entity(cacheable = false)
public abstract class AbstractParentNoCascade {

    @Key
    long id;

    @ForeignKey(delete = ReferentialAction.SET_NULL, update = ReferentialAction.RESTRICT)
    @OneToOne(cascade = {CascadeAction.NONE})
    ChildOneToOneNoCascade oneToOne;

    @ForeignKey(delete = ReferentialAction.SET_NULL, update = ReferentialAction.RESTRICT)
    @ManyToOne(cascade = {CascadeAction.NONE})
    ChildManyToOneNoCascade manyToOne;

    @OneToMany(cascade = {CascadeAction.NONE})
    List<ChildOneToManyNoCascade> oneToMany;

    @ManyToMany(cascade = {CascadeAction.NONE})
    @JunctionTable
    List<ChildManyToManyNoCascade> manyToMany;

}
