package io.requery.test;

import com.google.common.collect.BiMap;

import junit.framework.Assert;

import org.junit.Test;

import io.requery.Converter;


/**
 * Created by mluchi on 05/04/2017.
 */

public abstract class AbstractConverterTest<T extends Converter<FROM, TO>, FROM, TO> {

    public abstract T getConverter();

    public abstract BiMap<FROM, TO> getTestCases();

    @Test
    public void testConvertToPersisted() {
        BiMap<FROM, TO> testCases = getTestCases();
        for (FROM from : testCases.keySet()) {
            TO expectedConvertedValue = testCases.get(from);
            TO convertedValue = getConverter().convertToPersisted(from);

            assertEquals(expectedConvertedValue, convertedValue);
        }
    }

    @Test
    public void testConvertToMapped() {
        BiMap<TO, FROM> testCases = getTestCases().inverse();
        for (TO from : testCases.keySet()) {
            FROM expectedConvertedValue = testCases.get(from);
            FROM convertedValue = getConverter().convertToMapped(null, from);

            assertEquals(expectedConvertedValue, convertedValue);
        }
    }

    protected void assertEquals(Object obj1, Object obj2) {
        Assert.assertEquals(obj1, obj2);
    }

}
