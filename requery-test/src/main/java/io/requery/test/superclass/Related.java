package io.requery.test.superclass;

import io.requery.Entity;
import io.requery.Key;

@Entity(model = "superclass")
public interface Related {
    @Key
    public Long getId();
}
