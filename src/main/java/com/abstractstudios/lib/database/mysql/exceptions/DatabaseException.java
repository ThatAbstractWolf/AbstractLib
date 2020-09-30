package com.abstractstudios.lib.database.mysql.exceptions;

public class DatabaseException extends Exception {

    /**
     * Create a new instance of {@link DatabaseException}.
     * @param message - message.
     */
    public DatabaseException(String message) {
        super(message);
    }
}
