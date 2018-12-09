package io.requery.sql;

import io.requery.util.function.Predicate;

/**
 * Since SQL has no native support for Arrays and Iterables in "IN"-clauses,
 * this method inlines them. Example: <br/>
 * <br/>
 * <code>raw("select * from Person where id in :ids",
 * mapOf("ids" to Arrays.asList(1, 2, 3)));</code><br/>
 * <br/>
 * This is transformed into "select * from Person where id in (?, ?, ?) and the resulting new
 * parameters are (int, int, int) instead of (List).<br/>
 * Supported types to be inlined are {@link Iterable}s, Arrays of primitive
 * and reference types.
 */
public class NamedParameterInliner implements Predicate<Object[]> {
    
    @Override
    public boolean test(Object[] value) {
        return false;
    }
}
