package io.requery.test;

import io.requery.converter.EnumStringConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by DennisHartrampf on 01/03/2018.
 */
public class EnumStringConverterTest extends AbstractConverterTest<EnumStringConverter<EnumStringConverterTest.TestEnum>, EnumStringConverterTest.TestEnum, String> {

    enum TestEnum {
        CONSTANT_1, CONSTANT_2;

        @Override
        public String toString() {
            return "I cannot be used for persistence";
        }
    }

    @Override
    public EnumStringConverter<EnumStringConverterTest.TestEnum> getConverter() {
        return new EnumStringConverter<>(TestEnum.class);
    }

    @Override
    public Map<TestEnum, String> getTestCases() {
        Map<TestEnum, String> testCases = new HashMap<>();
        testCases.put(TestEnum.CONSTANT_1, "CONSTANT_1");
        testCases.put(TestEnum.CONSTANT_2, "CONSTANT_2");
        testCases.put(null, null);
        return testCases;
    }

    @Override
    protected Class<? extends TestEnum> getType() {
        return TestEnum.class;
    }

}
