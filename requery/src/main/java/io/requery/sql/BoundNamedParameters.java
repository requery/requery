package io.requery.sql;

import io.requery.query.Expression;
import io.requery.query.NamedExpression;
import io.requery.util.Objects;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;

/**
 * BoundNamedParameters
 *
 * @author debop (Sunghyouk Bae)
 * @since 18. 12. 7
 */
public class BoundNamedParameters extends BoundParameters {

    private final ArrayList<Expression<?>> expressions;
    private final ArrayList<Object> values;

    public BoundNamedParameters() {
        expressions = new ArrayList<>();
        values = new ArrayList<>();
    }

    public BoundNamedParameters(Map<String, Object> namedParameters) {
        this();
        for (Map.Entry<String, Object> entry : namedParameters.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            Class type = value == null ? Object.class : value.getClass();

            Expression expression = NamedExpression.of(name, type);
            add(expression, value);
        }
    }

    @Override
    public <V> void add(Expression<V> expression, V value) {
        expressions.add(expression);
        values.add(value);
    }

    @Override
    Expression<?> expressionAt(int index) {
        return expressions.get(index);
    }

    @Override
    Object valueAt(int index) {
        return values.get(index);
    }

    Expression<?> expressionByName(@Nonnull final String name) {
        for (Expression<?> expression : expressions) {
            if (Objects.equals(expression.getName(), name)) {
                return expression;
            }
        }
        return null;
    }

    int indexOfByName(@Nonnull final String name) {
        for (int i = 0; i < count(); i++) {
            if (Objects.equals(expressions.get(i).getName(), name)) {
                return i;
            }
        }
        return -1;
    }

    Object valueByName(@Nonnull final String name) {
        int index = indexOfByName(name);
        return (index < 0) ? null : values.get(index);
    }

    @Override
    public int count() {
        return expressions.size();
    }

    @Override
    public boolean isEmpty() {
        return count() == 0;
    }

    public void addAll(BoundNamedParameters parameters) {
        expressions.addAll(parameters.expressions);
        values.addAll(parameters.values);
    }

    @Override
    public void clear() {
        expressions.clear();
        values.clear();
    }

    ArrayList<String> getNames() {
        ArrayList<String> names = new ArrayList<>();

        for (Expression<?> expression : expressions) {
            names.add(expression.getName());
        }
        return names;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoundNamedParameters) {
            BoundNamedParameters parameters = (BoundNamedParameters) obj;
            return Objects.equals(getNames(), parameters.getNames()) &&
                   Objects.equals(values, parameters.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNames()) * 31 + Objects.hash(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count(); i++) {
            Expression expression = expressionAt(i);
            Object value = valueAt(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expression.getName()).append("=").append(value);
        }
        sb.append("]");
        return sb.toString();
    }
}
