package io.requery.test.jpa;


import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public interface Coordinate {
    @Column(nullable = false)
    float getLatitude();
    @Column(nullable = false)
    float getLongitude();
}
