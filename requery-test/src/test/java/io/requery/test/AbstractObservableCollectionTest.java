package io.requery.test;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import io.requery.proxy.CollectionChanges;
import io.requery.test.model.Phone;
import io.requery.util.ObservableCollection;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mluchi on 25/05/2017.
 */

public abstract class AbstractObservableCollectionTest<T extends Collection<Phone> & ObservableCollection<Phone>> {

    /**
     * Get the observable collection to test.
     *
     * @return The collection.
     */
    protected abstract T initObservableCollection();

    private Phone phone1;
    private Phone phone2;
    private T observableCollection;
    private CollectionChanges collectionChanges;

    @Before
    public void setUp() {
        // Populate the collection with 2 items
        observableCollection = initObservableCollection();
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
