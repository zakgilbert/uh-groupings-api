package edu.hawaii.its.api.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hydrate an object as you see fit. GenericServiceResult is a class
 * which will build a collection of arbitrary objects.
 */
public class GenericServiceResult {
    List<Object> data;
    Map<String, Integer> map;

    public GenericServiceResult() {
        this.data = new ArrayList<>();
        this.map = new HashMap<>();
    }

    /**
     * Initialize and add first object.
     *
     * @param key    of object.
     * @param object to add.
     */
    public GenericServiceResult(String key, Object object) {
        this();
        this.add(key, object);
    }

    /**
     * Initialize and add multiple objects.
     *
     * @param keys    - list of corresponding name values.
     * @param objects - a variable amount of arbitrary objects.
     */
    public GenericServiceResult(List<String> keys, Object... objects) {
        this();
        this.add(keys, objects);
    }

    /**
     * Add a variable amount of arbitrary objects, along with a list of their corresponding name values.
     * Example:
     * new GenericServiceResult(Arrays.asList("objA", "objB", "objC"), objA, objB, objC );
     *
     * @param keys    - list of corresponding name values.
     * @param objects - a variable amount of arbitrary objects.
     */
    public void add(List<String> keys, Object... objects) {
        Iterator<String> iter = keys.iterator();
        for (Object object : objects) {
            this.add(iter.next(), object);
        }
    }

    /**
     * Add a single object and key to response.
     *
     * @param key    a single key.
     * @param object a single arbitrary object.
     */
    public void add(String key, Object object) {
        this.data.add(object);
        this.map.put(key, this.data.indexOf(object));
    }

    /**
     * Get a object by key.
     *
     * @param key which was assigned, when object was added.
     * @return object that was added with key.
     */
    public Object get(String key) {
        return this.data.get(this.map.get(key));
    }

    /**
     * @return List<Object>data
     */
    public List<Object> getData() {
        return Collections.unmodifiableList(this.data);
    }

    /**
     * @return Map<String, Integer>map
     */
    public Map<String, Integer> getMap() {
        return Collections.unmodifiableMap(this.map);
    }

    public String toString() {
        Set<String> keys = this.map.keySet();
        Iterator<String> iter = keys.iterator();
        String str = "[ ";
        while (iter.hasNext()) {
            String key = iter.next();
            str += key + ": " + this.get(key) + "; ";
        }
        str += " ]";
        return str;
    }
}
