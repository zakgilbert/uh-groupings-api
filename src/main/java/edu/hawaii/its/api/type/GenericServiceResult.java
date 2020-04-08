package edu.hawaii.its.api.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hydrate an object as you see fit. GenericServiceResult is a class
 * which will build a collection of arbitrary objects.
 */
public class GenericServiceResult {
    // Storage of arbitrary objects.
    List<Object> data;
    // Storage of names and indices of each objects added.
    Map<String, Integer> map;
    int size;

    public GenericServiceResult() {
        this.data = new ArrayList<>();
        this.map = new HashMap<>();
        this.size = 0;
    }

    /**
     * Initialize and add first object.
     *
     * @param prop
     * @param object
     */
    public GenericServiceResult(String prop, Object object) {
        this();
        this.add(prop, object);
    }

    /**
     * Initialize and add multiple objects.
     *
     * @param props   - list of corresponding name values.
     * @param objects - a variable amount of arbitrary objects.
     */
    public GenericServiceResult(List<String> props, Object... objects) {
        this();
        this.add(props, objects);
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
        this.map.put(key, this.size);
        this.size++;
    }

    public List<Object> getData() {
        return Collections.unmodifiableList(this.data);
    }

    public Map<String, Integer> getMap() {
        return Collections.unmodifiableMap(this.map);
    }
}
