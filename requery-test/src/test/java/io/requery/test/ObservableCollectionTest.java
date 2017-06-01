package io.requery.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.requery.proxy.CollectionChanges;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.ObservableCollection;
import io.requery.util.ObservableList;
import io.requery.util.ObservableSet;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mluchi on 25/05/2017.
 */
@RunWith(Parameterized.class)
public class ObservableCollectionTest<T extends Collection<Phone> & ObservableCollection<Phone>> {

    @Parameterized.Parameters
    public static <T extends Collection<Phone> & ObservableCollection<Phone>> Collection<T> observableCollections() {
        Person person = new Person();

        // ObservableList
        List<Phone> observableList = person.getPhoneNumbersList();
        assertTrue(observableList instanceof ObservableList);

        // ObservableSet
        Set<Phone> observableSet = person.getPhoneNumbersSet();
        assertTrue(observableSet instanceof ObservableSet);

        return Arrays.asList((T) observableList, (T) observableSet);
    }

    private T observableCollection;
    private Phone phone1;
    private Phone phone2;
    private CollectionChanges collectionChanges;

    public ObservableCollectionTest(T observableCollection) {
        this.observableCollection = observableCollection;
    }

    @Before
    public void setUp() {
        // Populate the collection with 2 items
        observableCollection.clear();
        phone1 = new Phone();
        phone1.setPhoneNumber("1");
        phone2 = new Phone();
        phone2.setPhoneNumber("2");
        observableCollection.add(phone1);
        observableCollection.add(phone2);

        // Make sure that initial status of Observable collection is clear (no elements added or removed)
        assertTrue(observableCollection.observer() instanceof CollectionChanges);
        collectionChanges = (CollectionChanges) observableCollection.observer();
        collectionChanges.clear();
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().isEmpty());
    }

    /**
     * Tests for issue https://github.com/requery/requery/issues/569
     */
    @Test
    public void testClear() {
        // Add an element to the collection, then clear the collection
        Phone phone3 = new Phone();
        phone3.setPhoneNumber("3");
        observableCollection.add(phone3);
        observableCollection.clear();

        // Assert that the collection changes do not contain the phone3 item (add+remove=nothing) and contains the removals of phone1 and phone2
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().size() == 2);
        assertTrue(collectionChanges.removedElements().contains(phone1));
        assertTrue(collectionChanges.removedElements().contains(phone2));
        assertFalse(collectionChanges.removedElements().contains(phone3));
    }

    /**
     * Tests for issue https://github.com/requery/requery/issues/569
     */
    @Test
    public void testRemoveUsingIterator() {
        // Remove all items using iterator
        Iterator iterator = observableCollection.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }

        // Assert that collection changes contains the removed items
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().size() == 2);
        assertTrue(collectionChanges.removedElements().contains(phone1));
        assertTrue(collectionChanges.removedElements().contains(phone2));
    }

}
