package io.requery.util;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Created by mluchi on 25/05/2017.
 */

/**
 * The iterator of an {@link ObservableCollection}.
 */
class ObservableCollectionIterator<T> implements Iterator<T> {

    private final Iterator<T> iterator;
    private final CollectionObserver<T> observer;
    private T lastObject;

    /**
     * Create a new iterator of an ObservableCollection.
     *
     * @param collection The collection hold by the ObservableCollection (not the
     *                   ObservableCollection itself).
     * @param observer   The observer of the collection.
     */
    public ObservableCollectionIterator(@Nonnull Collection<T> collection, @Nullable CollectionObserver<T> observer) {
        this.iterator = collection.iterator();
        this.observer = observer;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        lastObject = iterator.next();
        return lastObject;
    }

    @Override
    public void remove() {
        iterator.remove(); //NOTE: this already throws IllegalStateException if next() has never been called
        if (observer != null && lastObject != null) {
            observer.elementRemoved(lastObject);
        }
    }

}
