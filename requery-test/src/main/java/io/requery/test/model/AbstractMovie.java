package io.requery.test.model;


import io.requery.Entity;
import io.requery.Key;

@Entity
public class AbstractMovie {

    @Key
    protected int id;

    protected String title;

    public AbstractMovie() {
    }

    public AbstractMovie(final int id, final String title) {
        this.id = id;
        this.title = title;
    }

}
