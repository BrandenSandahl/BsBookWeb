package com.Sixtel;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * Created by branden on 3/1/16 at 14:11.
 */
public class MainTest {


    //create the tables to work with
    public Connection startConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./test"); //define a different DB
        Main.createTables(conn);
        return conn;
    }

    //kill the tables so we have fresh data for new tests
    public  void endConnection(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE user");
        stmt.execute("DROP TABLE book");
        conn.close();
    }

    @Test
    public  void testUser() throws SQLException {
        Connection conn = startConnection();

        Main.createUser(conn, "Zach", "Oakes");
        User u = Main.selectUser(conn, "Zach");
        endConnection(conn);

        assertTrue(u != null);
    }

    @Test
    public void testBook() throws SQLException {
        Connection conn = startConnection();

        Main.createUser(conn, "Zach", "Oakes");
        User u = Main.selectUser(conn, "Zach");

        Main.createBook(conn, "title", "author", "description", 8583, 1999, 'A', 1);
        Book b = Main.selectBook(conn, 1);

        endConnection(conn);

        assertTrue(b != null);
    }



}