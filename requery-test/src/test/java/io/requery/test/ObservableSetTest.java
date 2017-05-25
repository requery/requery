package io.requery.test;

import java.util.Collection;
import java.util.Set;

import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.ObservableSet;

import static org.junit.Assert.assertTrue;

/**
 * Created by mluchi on 23/05/2017.
 */

public class ObservableSetTest extends AbstractObservableCollectionTest {

    @Override
    protected Collection<Phone> initObservableCollection() {
        Person person = new Person();
        Set<Phone> collection = person.getPhoneNumbersSet();
        assertTrue(collection instanceof ObservableSet);
        return collection;
    }

}
