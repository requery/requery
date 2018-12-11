package io.requery.sql;

import io.requery.PersistenceException;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.query.BaseResult;
import io.requery.query.Result;
import io.requery.util.CloseableIterator;
import io.requery.util.function.Predicate;
import io.requery.util.function.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Raw Query with Named parameters
 *
 * @author debop (Sunghyouk Bae)
 * @since 18. 12. 8
 */
public class RawEntityNamedQuery<E extends S, S> extends PreparedQueryOperation implements Supplier<Result<E>> {

    private final EntityReader<E, S> reader;
    private final Type<E> type;
    private final BoundParameters boundParameters;
    private final String sql;

    RawEntityNamedQuery(EntityContext<S> context, Class<E> cls, String sql, Map<String, Object> namedParameters) {
        super(context, null);

        NamedParameterParser inlined = new NamedParameterParser(sql, namedParameters).apply();
        this.type = configuration.getModel().typeOf(cls);
        this.sql = inlined.sql();
        this.reader = context.read(cls);
        this.boundParameters = new BoundParameters(inlined.parameters());
    }

    @Override
    public Result<E> get() {
        PreparedStatement statement = null;
        try {
            Connection connection = configuration.getConnection();
            statement = prepare(sql, connection);
            mapParameters(statement, boundParameters);
            return new EntityResult(statement);
        } catch (Exception e) {
            throw StatementExecutionException.closing(statement, e, sql);
        }
    }

    private class EntityResult extends BaseResult<E> {

        private final PreparedStatement statement;

        private EntityResult(PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public CloseableIterator<E> createIterator(int skip, int take) {
            try {
                StatementListener listener = configuration.getStatementListener();
                listener.beforeExecuteQuery(statement, sql, boundParameters);
                ResultSet results = statement.executeQuery();
                listener.afterExecuteQuery(statement);
                // read the result meta data
                ResultSetMetaData metadata = results.getMetaData();
                // map of entity column names to attributes
                Map<String, Attribute<E, ?>> map = new HashMap<>();
                for (Attribute<E, ?> attribute : type.getAttributes()) {
                    map.put(attribute.getName().toLowerCase(Locale.ROOT), attribute);
                }
                Set<Attribute<E, ?>> attributes = new LinkedHashSet<>();
                for (int i = 0; i < metadata.getColumnCount(); i++) {
                    String name = metadata.getColumnName(i + 1);
                    Attribute<E, ?> attribute = map.get(name.toLowerCase(Locale.ROOT));
                    if (attribute != null) {
                        attributes.add(attribute);
                    }
                }
                Attribute[] array = Attributes.toArray(attributes,
                                                       new Predicate<Attribute<E, ?>>() {
                                                           @Override
                                                           public boolean test(Attribute<E, ?> value) {
                                                               return true;
                                                           }
                                                       });
                EntityResultReader<E, S> entityReader = new EntityResultReader<>(reader, array);
                return new ResultSetIterator<>(entityReader, results, null, true, true);
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }
    }
}
