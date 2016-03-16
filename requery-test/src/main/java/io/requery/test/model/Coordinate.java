package io.requery.test.model;


import io.requery.Column;
import io.requery.Lazy;
import io.requery.Superclass;

@Superclass
public class Coordinate {
    @Lazy
    @Column(value = "0.0", nullable = false)
    protected float latitude;
    @Lazy
    @Column(value = "0.0", nullable = false)
    protected float longitude;
}
