package io.requery.converter;

import java.util.Currency;

import javax.annotation.Nullable;

import io.requery.Converter;

/**
 * A {@link Converter} between a {@link Currency} and its ISO 4217 code.
 */
public class CurrencyConverter implements Converter<Currency, String> {

    /**
     * The maximum length of the ISO 4217 code of available currencies (determines the size of
     * database field).
     */
    public static final int PERSISTED_SIZE = 3;

// Commented because it requires API level 14
//    static {
//        int max = 0;
//        for (Currency currency : Currency.getAvailableCurrencies()) {
//            max = Math.max(max, currency.getCurrencyCode().length());
//        }
//        PERSISTED_SIZE = max;
//    }

    @Override
    public Class<Currency> getMappedType() {
        return Currency.class;
    }

    @Override
    public Class<String> getPersistedType() {
        return String.class;
    }

    @Nullable
    @Override
    public Integer getPersistedSize() {
        return PERSISTED_SIZE;
    }

    @Override
    public String convertToPersisted(Currency value) {
        return value == null ? null : value.getCurrencyCode();
    }

    @Override
    public Currency convertToMapped(Class<? extends Currency> type, @Nullable String value) {
        return value == null ? null : Currency.getInstance(value);
    }

}
