package com.abstractstudios.lib.callback;

/**
 * Create a new callback instance.
 * @param <T> - datatype.
 */
public interface Callback<T> {

    /**
     * Return the object.
     * @param obj - object.
     */
    void call(T obj);
}
