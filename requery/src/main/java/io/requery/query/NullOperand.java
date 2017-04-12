package io.requery.query;

import io.requery.util.Objects;

public class NullOperand<L, R> implements Condition<L, R> {

    @Override
    public L getLeftOperand() {
        return null;
    }

    @Override
    public Operator getOperator() {
        return null;
    }

    @Override
    public R getRightOperand() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof NullOperand) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass());
    }

}
