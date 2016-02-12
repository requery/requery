package io.requery.test;

import io.requery.util.ClassMap;
import org.junit.Assert;
import org.junit.Test;

public class ClassMapTest {

    @Test
    public void testDerivedContains() {
        ClassMap<String> map = new ClassMap<>();
        map.put(CharSequence.class, "test");
        Assert.assertTrue(map.containsKey(String.class));
        Assert.assertEquals(map.get(String.class), "test");
        Assert.assertNull(map.get(Object.class));
    }
}
