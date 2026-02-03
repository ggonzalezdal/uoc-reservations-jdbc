package edu.uoc;

import edu.uoc.db.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        try (Connection conn = Database.getConnection()) {

            System.out.println("Connected to database!");
            System.out.println("DB product   : " + conn.getMetaData().getDatabaseProductName());
            System.out.println("DB version   : " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("JDBC driver  : " + conn.getMetaData().getDriverName());
            System.out.println("Driver ver.  : " + conn.getMetaData().getDriverVersion());

        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
        }
    }
}
