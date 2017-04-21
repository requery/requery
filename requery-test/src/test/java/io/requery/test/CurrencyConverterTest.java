package io.requery.test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Currency;

import io.requery.converter.CurrencyConverter;

/**
 * Created by mluchi on 05/04/2017.
 */

public class CurrencyConverterTest extends AbstractConverterTest<CurrencyConverter, Currency, String> {

    @Override
    public CurrencyConverter getConverter() {
        return new CurrencyConverter();
    }

    @Override
    public BiMap<Currency, String> getTestCases() {
        BiMap<Currency, String> testCases = HashBiMap.create();
//        for (Currency currency : Currency.getAvailableCurrencies()) {
//            testCases.put(currency, currency.getCurrencyCode());
//        }
        testCases.put(Currency.getInstance("EUR"), "EUR");
        testCases.put(Currency.getInstance("HKD"), "HKD");
        testCases.put(Currency.getInstance("USD"), "USD");
        testCases.put(null, null);
        return testCases;
    }

}
