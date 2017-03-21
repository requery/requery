package io.requery.test.model3;

import io.requery.Embedded;
import io.requery.Entity;
import io.requery.Key;

@Entity(model = "model3")
public interface Location {
    @Key
    int getId();

    @Embedded
    Address getAddress();
}
