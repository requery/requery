package io.requery.test.immutables;

import io.requery.Column;
import io.requery.Entity;
import io.requery.Generated;
import io.requery.Key;
import org.immutables.value.Value;

@Value.Immutable
@Entity(model = "immutable")
public abstract class Address {

    public static ImmutableAddress.Builder builder() {
        return ImmutableAddress.builder();
    }

    @Key @Generated
    public abstract int getId();

    public abstract String getLine1();
    public abstract String getLine2();
    public abstract String getState();

    @Column(length = 5)
    public abstract String getZip();

    @Column(length = 2)
    public abstract String getCountry();

    public abstract String getCity();
}
