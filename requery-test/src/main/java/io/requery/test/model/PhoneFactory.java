package io.requery.test.model;

import io.requery.util.function.Supplier;

public class PhoneFactory implements Supplier<Phone> {

    @Override
    public Phone get() {
        // not doing anything special here
        return new Phone("");
    }
}
