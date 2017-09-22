package io.requery.test.model;


import java.util.List;

import io.requery.CascadeAction;
import io.requery.Column;
import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToMany;

/**
 * Created by mluchi on 03/08/2017.
 */
@Entity(cacheable = false)
public abstract class AbstractChildManyToManyNoCascade {

    @Key
    long id;

    @Column
    String attribute;

    @ManyToMany(mappedBy = "manyToMany", cascade = CascadeAction.NONE)
    List<ParentNoCascade> parents;

}
