package io.requery.test.superclass;

import io.requery.JunctionTable;
import io.requery.Key;
import io.requery.ManyToMany;
import io.requery.Superclass;

import java.util.List;

@Superclass
public interface Base {
    @Key
    Long getId();

    @ManyToMany
    @JunctionTable
    List<Related> getRelated();
}
