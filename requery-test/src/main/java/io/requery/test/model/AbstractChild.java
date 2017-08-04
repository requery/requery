package io.requery.test.model;

import io.requery.Entity;
import io.requery.Key;

/**
 * Created by mluchi on 04/08/2017.
 */

@Entity
public abstract class AbstractChild {

    @Key
    long id;

}
