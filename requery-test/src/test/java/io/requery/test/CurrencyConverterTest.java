package io.requery.test;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

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
    public Map<Currency, String> getTestCases() {
        Map<Currency, String> testCases = new HashMap<>();
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
