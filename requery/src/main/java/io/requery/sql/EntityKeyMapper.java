package io.requery.sql;

import io.requery.meta.Attribute;
import io.requery.meta.Type;

/**
 * @see #mapEntitiesToKeys(RuntimeConfiguration, Object[])
 */
final class EntityKeyMapper {

    private EntityKeyMapper() {
    }

    /**
     * Map objects that are database entities with a single key in the parameter array to
     * their single key.
     * <p>
     * Modifies the supplied array.
     */
    static void mapEntitiesToKeys(RuntimeConfiguration configuration, Object[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            boolean isEntity = configuration.model().containsTypeOf(param.getClass());
            if (isEntity) {
                Type<Object> objectType = configuration.model().typeOf(param.getClass());
                Attribute<Object, ?> singleKeyAttribute = objectType.singleKeyAttribute();
                if (singleKeyAttribute != null) {
                    Object key = singleKeyAttribute.property().get(param);
                    parameters[i] = key;
                }
            }
        }
    }

}
