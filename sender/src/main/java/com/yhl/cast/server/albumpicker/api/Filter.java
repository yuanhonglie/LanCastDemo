package com.yhl.cast.server.albumpicker.api;

public interface Filter<T> {

    /**
     * Filter the file.
     */
    boolean filter(T attributes);

}