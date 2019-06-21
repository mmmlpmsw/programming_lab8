package ru.n4d9.server;

import java.util.HashMap;

public class Context {
    private static HashMap<String, Object> objects = new HashMap<>();

    public static Object get(String key) {
        return objects.get(key);
    }

    public static void set(String key, Object value) {
        objects.put(key, value);
    }

}
