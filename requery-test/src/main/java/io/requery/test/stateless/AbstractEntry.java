package io.requery.test.stateless;

import io.requery.Entity;
import io.requery.Key;

import java.util.Date;

@Entity(stateless = true, model = "stateless")
public abstract class AbstractEntry {
    @Key
    String id;
    boolean flag1;
    boolean flag2;
    Date created;
}
