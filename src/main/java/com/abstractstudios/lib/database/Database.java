package com.abstractstudios.lib.database;

public interface Database<T> {

    void connect();

    void disconnect();

    T getDatabase();
}
