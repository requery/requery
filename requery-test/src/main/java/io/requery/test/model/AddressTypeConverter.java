package io.requery.test.model;

import io.requery.converter.EnumOrdinalConverter;

public class AddressTypeConverter extends EnumOrdinalConverter<AddressType> {

    public AddressTypeConverter() {
        super(AddressType.class);
    }
}
