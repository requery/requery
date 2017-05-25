package io.requery.test;

import java.util.Collection;
import java.util.List;

import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.ObservableList;

import static org.junit.Assert.assertTrue;

/**
 * Created by mluchi on 23/05/2017.
 */

public class ObservableListTest extends AbstractObservableCollectionTest {

    @Override
    protected Collection<Phone> initObservableCollection() {
        Person person = new Person();
        List<Phone> collection = person.getPhoneNumbersList();
        assertTrue(collection instanceof ObservableList);
        return collection;
    }

}
