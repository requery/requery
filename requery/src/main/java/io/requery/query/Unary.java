package io.requery.query;

public class Unary {

    public static <Q> Q not(AndOr<Q> subject) {
        return subject.not();
    }

}
