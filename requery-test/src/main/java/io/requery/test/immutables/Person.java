package io.requery.test.immutables;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import org.immutables.value.Value;

import java.util.Date;
import java.util.UUID;

@Value.Immutable
@Entity(model = "immutable", builder = ImmutablePerson.Builder.class)
public abstract class Person {

    @Key @Generated
    public abstract int getId();

    public abstract String getName();
    public abstract String getEmail();
    public abstract Date getBirthday();
    public abstract int getAge();

    public abstract String getAbout();

    @Value.Default
    @Column(unique = true)
    public UUID getUUID() {
        return UUID.randomUUID();
    }
}
