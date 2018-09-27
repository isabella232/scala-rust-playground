package example;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface RustInterfaceJna extends Library {
    Pointer newConnection(String url);

    void startTransaction(Pointer connection);

    void commitTransaction(Pointer connection);

    void rollbackTransaction(Pointer connection);

    void closeConnection(Pointer connection);

    void sqlExecute(Pointer connection, String query, String params);

    String sqlQuery(Pointer connection, String query, String params);
}

