package io.requery.sql;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Parser for query with Named parameter
 */
public class NamedParameterParser {

    private static Logger logger = Logger.getLogger("NamedParameterParser");
    private static final Pattern questionMarkPattern = Pattern.compile("\\?");

    private String sql;
    private Object[] parameters;
    private final Map<String, Object> namedParameters;
    private final Map<String, List<Integer>> nameIndexesMap;

    NamedParameterParser(@Nonnull String sql, Map<String, Object> namedParameters) {
        this.sql = sql;
        this.parameters = new Object[0];
        this.namedParameters = namedParameters;
        this.nameIndexesMap = new HashMap<>();
    }

    public Object[] parameters() {
        return parameters;
    }

    public String sql() {
        return this.sql;
    }

    NamedParameterParser apply() {

        // Parse named query to replace parameter name to `?`
        this.sql = parse(sql, nameIndexesMap);

        int parameterCount = getParameterCount(nameIndexesMap);
        this.parameters = new Object[parameterCount];

        // matching parameter values
        for (Entry<String, List<Integer>> entry : nameIndexesMap.entrySet()) {
            String name = entry.getKey();
            List<Integer> indexes = entry.getValue();

            Object value = namedParameters.get(name);

            for (int index : indexes) {
                parameters[index - 1] = value;
            }
        }

        ParameterInliner inliner = new ParameterInliner(sql, parameters).apply();

        this.sql = inliner.sql();
        this.parameters = inliner.parameters();

        logger.log(Level.INFO, "Named SQL parsed sql={0}, parameters={1}", new Object[] { this.sql, this.parameters });

        return this;
    }

    private int getParameterCount(Map<String, List<Integer>> indexMap) {
        int count = 0;
        for (Collection<Integer> indexes : indexMap.values()) {
            count += indexes.size();
        }
        return count;
    }

    @Nonnull
    static String parse(@Nonnull final String query, @Nonnull final Map<String, List<Integer>> paramMap) {
        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        int i = 0;
        while (i < length) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    List<Integer> indexList = paramMap.get(name);
                    if (indexList == null) {
                        indexList = new ArrayList<>();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }
            parsedQuery.append(c);
            i++;
        }


        String parsed = parsedQuery.toString();
        logger.log(Level.INFO, "Parsed SQL= {0}", parsed);
        return parsed;
    }
}
