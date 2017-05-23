package io.requery.test;

import org.junit.Before;
import org.junit.Test;

import io.requery.proxy.CollectionChanges;
import io.requery.test.model.Person;
import io.requery.test.model.Phone;
import io.requery.util.ObservableList;

import static org.junit.Assert.assertTrue;

/**
 * Created by mluchi on 23/05/2017.
 */

/**
 * Tests for issue https://github.com/requery/requery/issues/568
 */
public class CollectionChangesTest {

    private Person person;
    private Phone phone1;
    private Phone phone2;
    private ObservableList observableList;
    private CollectionChanges collectionChanges;

    @Before
    public void setUp() {
        // Create some mock objects (Person has a to-many relationship to Phone)
        phone1 = new Phone();
        phone2 = new Phone();
        person = new Person();
        person.getPhoneNumbersList().add(phone1);
        person.getPhoneNumbersList().add(phone2);

        // Make sure that initial status of Observable collection is clear (no elements added or removed)
        assertTrue(person.getPhoneNumbersList() instanceof ObservableList);
        observableList = (ObservableList) person.getPhoneNumbersList();
        assertTrue(observableList.observer() instanceof CollectionChanges);
        collectionChanges = (CollectionChanges) observableList.observer();
        collectionChanges.clear();
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().isEmpty());
    }

    @Test
    public void testAddingElementThatWasPreviouslyRemoved() {
        Phone phone = new Phone();
        observableList.remove(phone1);
        assertTrue(collectionChanges.removedElements().contains(phone1));
        observableList.add(phone1);
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().isEmpty());
    }

    @Test
    public void testRemovingElementThatWasPreviouslyAdded() {
        Phone phone = new Phone();
        observableList.add(phone);
        assertTrue(collectionChanges.addedElements().contains(phone));
        observableList.remove(phone);
        assertTrue(collectionChanges.addedElements().isEmpty());
        assertTrue(collectionChanges.removedElements().isEmpty());
    }

}
