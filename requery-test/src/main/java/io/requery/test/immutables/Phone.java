package io.requery.test.immutables;

import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import org.immutables.value.Value;

@Value.Immutable
@Entity(model = "immutable")
public abstract class Phone {

    public static ImmutablePhone.Builder builder() {
        return ImmutablePhone.builder();
    }

    @Key @Generated
    public abstract int getId();
    public abstract String getPhoneNumber();
    public abstract boolean isNormalized();

    @ForeignKey(references = Person.class)
    public abstract int getOwnerId();

    // this method should not be processed
    public boolean isValid() {
        return getPhoneNumber() != null && isNormalized();
    }
}
