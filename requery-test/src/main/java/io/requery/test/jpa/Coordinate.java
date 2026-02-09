package io.requery.test.jpa;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public interface Coordinate {
    @Column(nullable = false)
    float getLatitude();
    @Column(nullable = false)
    float getLongitude();
}
