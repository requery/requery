package io.requery.test.model3;

import io.requery.Column;
import io.requery.Embedded;

@Embedded
public interface Address {
    @Column
    String getAddress();
}
