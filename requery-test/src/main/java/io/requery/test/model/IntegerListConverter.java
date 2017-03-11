package io.requery.test.model;

import io.requery.Converter;

import java.util.ArrayList;

class IntegerListConverter implements Converter<ArrayList<Integer>, String> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<ArrayList<Integer>> getMappedType() {
        return (Class)ArrayList.class;
    }

    @Override
    public Class<String> getPersistedType() {
        return String.class;
    }

    @Override
    public Integer getPersistedSize() {
        return null;
    }

    @Override
    public String convertToPersisted(ArrayList<Integer> value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Object integer : value) {
            if (index > 0) {
                sb.append(",");
            }
            sb.append(integer);
            index++;
        }
        return sb.toString();
    }

    @Override
    public ArrayList<Integer> convertToMapped(Class<? extends ArrayList<Integer>> type,
                                              String value) {
        ArrayList<Integer> list = new ArrayList<>();
        if (value != null) {
            String[] parts = value.split(",");
            for (String part : parts) {
                if (part.length() > 0) {
                    list.add(Integer.parseInt(part.trim()));
                }
            }
        }
        return list;
    }
}
