package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;


import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {
    static boolean passMisMatch = false; //this controls an HTML div. maybe look into better way to do this



    /** Begin DB stuff
       Methods for DB interactions
    */

    public static void createTables(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS user (user_id IDENTITY, user_name VARCHAR, user_password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS book (" +
                "book_id IDENTITY, book_user_id INT, book_title VARCHAR, book_author VARCHAR, book_description VARCHAR, " +
                "isbn INT, book_year INT, book_rating CHAR )");
    }

    public static void createUser(Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO user VALUES (null, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }

    public static void createBook(Connection conn,String title, String author, String description,
                                  int isbn, int year, char rating, int owner_id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO book VALUES (null, ?, ?, ?, ?, ?, ?, ?)");
        stmt.setInt(1, owner_id); //remember this is going on here. this shit is going on. it's going down. it's happening.
        stmt.setString(2, title);
        stmt.setString(3, author);
        stmt.setString(4, description);
        stmt.setInt(5, isbn);
        stmt.setInt(6, year);
        String ratingString = String.valueOf(rating);
        stmt.setString(7, ratingString);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user WHERE user_name = ?");
        stmt.setString(1, userName);

        ResultSet results = stmt.executeQuery();

        if (results.next()) {
            int id = results.getInt("user_id");
            String name = results.getString("user_name");
            String password = results.getString("user_password");

            User u = new User(name, password);
            u.setId(id);
            return  u;
        }
        return null;
    }

    public static Book selectBook(Connection conn, int bookId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM book INNER JOIN user ON book_user_id = user_id WHERE book_id = ?");
        stmt.setInt(1, bookId);

        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            String title = results.getString("book_title");
            String author = results.getString("book_author");
            String description = results.getString("book_description");
            int isbn = results.getInt("isbn");
            int year = results.getInt("book_year");
            char rating = results.getString("book_rating").charAt(0);
            String owner = results.getString("user.user_name");

            Book b = new Book(title, author, description, isbn, year, rating, owner);
            b.setBookId(bookId);
            return b;
        }
        return null;
    }

    public static ArrayList<Book> selectBooks(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        ArrayList<Book> booksList = new ArrayList<>();

        ResultSet results = stmt.executeQuery("SELECT * FROM book INNER JOIN user ON book_user_id = user_id");

        while (results.next()) {
            String title = results.getString("book_title");
            String author = results.getString("book_author");
            String description = results.getString("book_description");
            int isbn = results.getInt("isbn");
            int year = results.getInt("book_year");
            char rating = results.getString("book_rating").charAt(0);
            String owner = results.getString("user.user_name");
            int bookId = results.getInt("book_id");

            Book b = new Book(title, author, description, isbn, year, rating, owner);
            b.setBookId(bookId);
            booksList.add(b);
        }
        return booksList;
    }

    public static int editBook(Connection conn, int bookId, String title, String author, String description,
                                int isbn, int year, char rating) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE book SET " +
                " book_title = ?, book_author = ?, book_description = ?, isbn = ?, book_year = ?, book_rating = ?" +
                "WHERE book_id = ?");

        stmt.setString(1, title);
        stmt.setString(2, author);
        stmt.setString(3, description);
        stmt.setInt(4, isbn);
        stmt.setInt(5, year);
        String ratingString = String.valueOf(rating);
        stmt.setString(6, ratingString);
        stmt.setInt(7, bookId);
        int updateCount = stmt.executeUpdate();  //gives me a number of items that were updated. Not using this now. Just wanted to do it.
        return updateCount;
    }

    public static void deleteBook(Connection conn, int bookId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE book WHERE book_id = ?");
        stmt.setInt(1, bookId);
        stmt.execute();
    }

    public static int getUserId(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM user WHERE user_name = ?");
        stmt.setString(1, userName);

        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int userId = results.getInt("user_id");
            return userId;
        }

        return -1; //i guess maybe i'm just doing this for possible future error checking?
    }



    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Spark.externalStaticFileLocation("public"); //link to location of CSS files

        createTables(conn);

        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");


                    HashMap m = new HashMap();
                    m.put("passMisMatch", passMisMatch);

                    //Array for mustache display purposes
                    ArrayList<Book> bookListEditable = new ArrayList<>();
                    bookListEditable = selectBooks(conn);

                    boolean haveBook = !bookListEditable.isEmpty();  //hides the book table if there are no items in it

                    //go through the list and set the ownership flag for editing purposes.
                    for (Book book : bookListEditable) {
                        book.setIsOwner(book.getOwnerName().equals(userName));
                    }
                    Collections.sort(bookListEditable);

//                    bookListEditable = bookListEditable.stream().sorted()
//                            .map((book -> book.setIsOwner(book.getOwnerName().equals(userName)))
//                            .collect(Collectors.toList());






                    //pagination stuff
                    String nextString = request.queryParams("offset"); //this only sends when user directly interacts with something that causes it to send.
                    int subStart = 0; //0 if it's nothing else
                    if (nextString != null) {   //this starts out null because it's not sending anything until you click one of the two.
                        subStart = Integer.parseInt(nextString);
                    }
                    int subTo = subStart + 5;

                    //this will keep the array from going out of bounds. I'm just constraining the sub method to the size of the array
                    if (subTo > bookListEditable.size()) {
                        subTo = bookListEditable.size();
                    }



                    //run through a method that checks to see if logged in user is the owner of the book, and allows editing and deletion.
                    bookListEditable = getBookOwnership(userName, bookListEditable);
                    m.put("bookList", bookListEditable.subList(subStart, subTo));
                    m.put("haveBook", haveBook);
                    //next and prev anchors
                    m.put("next", ((subTo != (bookListEditable.size())) ? subStart + 5 : null));  //adjust next link, hide if the end of my subList is at the end of the ArrayList
                    m.put("previous", (subStart != 0) ? subStart - 5 : null); //adjust previous link, hide if it's at 0.


                    if (userName != null) {
                        m.put("userName", userName);
                        return new ModelAndView(m, "home.html");
                    } else {
                        return new ModelAndView(m, "home.html");
                    }
                }),
        new MustacheTemplateEngine()
        );


        Spark.get(
                "/view",
                ((request1, response1) -> {
                    //get the book object the user clicked on
                    int bookId = Integer.valueOf(request1.queryParams("bookId"));

                    Book b = selectBook(conn, bookId );

                    HashMap m = new HashMap();
                    m.put("book", b); //place book in the map so we can populate a page with the books info.
                    return new ModelAndView(m, "view.html");
                }),
                new MustacheTemplateEngine()
        );


        Spark.get(
                "/edit",
                ((request1, response1) -> {
                    Session session = request1.session();
                    String userName = session.attribute("userName");

                    if (userName == null) {
                            throw  new  Exception("Someone is trying to edit a book without authorization. They are not authorized. There is no authorization.");
                    }

                    //get the book object the user clicked on
                    int bookId = Integer.valueOf(request1.queryParams("bookId"));
//                    Session session = request1.session();
//                    session.attribute("bookId", bookId);  //add book into session

                    Book b = selectBook(conn, bookId);

                    HashMap m = new HashMap();

                    m.put("book", b); //place book in the map so we can populate a page with the books info.
                    m.put("userName", userName);
                    return new ModelAndView(m, "edit.html");
                }),
        new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                ((request, response) -> {
                    String name = request.queryParams("nameInput");
                    String pass = request.queryParams("passwordInput");

                    User user = selectUser(conn, name);


                    if (user != null && (user.getUserPassword().equals(pass))) {  //if exist and the pass matches
                        //create session for user
                        Session session = request.session();
                        session.attribute("userName", name);

                        response.redirect("/");
                        return "";
                    } else if (user == null)  {   //if the user does not yet exist
                            //add the user to the map
                            createUser(conn, name, pass);
                            //create session for user
                            Session session = request.session();
                            session.attribute("userName", name);

                            response.redirect("/");
                            return "";

                    } else {  //otherwise just go back to index because the user entered bad pass
                        passMisMatch = true;  //this turns the password mismatch HTML on and off

                        response.redirect("/");
                        return "";
                    }
                })
        );

        Spark.post(
                "/enter-item",
                ((request, response) -> {
                    String userName = request.session().attribute("userName");

                    //lets just grab all the fields first off. worry about error checking later yo.
                    String title = request.queryParams("bookTitleInput");
                    String author = request.queryParams("bookAuthorInput");
                    String description = request.queryParams("bookDescriptionInput");
                    char rating = request.queryParams("bookRatingInput").charAt(0);
                    int year = Integer.parseInt(request.queryParams("bookYearInput"));
                    //for some reason i could not do this all in one go ??
                    String isbnString = request.queryParams("bookIsbnInput");
                    int isbn = Integer.parseInt(isbnString);

                    //add the book to the DB
                    int userId = getUserId(conn, userName);
                    createBook(conn, title, author, description, isbn, year, rating, userId);

                    response.redirect("/");
                    return "";
                })
        );


        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/edit-item",
                ((request, response) -> {
                    String userName = request.session().attribute("userName");

                    //get all my fields
                    int bookId = Integer.valueOf(request.queryParams("bookId"));
                    String title = request.queryParams("bookTitleInput");
                    String author = request.queryParams("bookAuthorInput");
                    String description = request.queryParams("bookDescriptionInput");
                    char rating = request.queryParams("bookRatingInput").charAt(0);
                    int year = Integer.parseInt(request.queryParams("bookYearInput"));
                    int isbn = Integer.parseInt(request.queryParams("bookIsbnInput"));

                    editBook(conn, bookId, title, author, description, isbn, year, rating);

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/delete",
                ((request1, response1) -> {
                    int bookId = Integer.parseInt(request1.queryParams("bookId"));

                    deleteBook(conn, bookId);

                    response1.redirect("/");
                    return "";
                })
        );
    }

    //stream function that will set an editable field within Book
    //this runs via mustaches interaction behavior with an ArrayList
    static ArrayList<Book> getBookOwnership(String userName, ArrayList<Book> bookList) {
        bookList = bookList.stream()
                .map((book) -> {
                    if (userName != null) {
                        book.setIsOwner(userName.equals(book.getOwnerName()));
                        return book;
                    } else {
                        book.setIsOwner(false);
                        return book;
                    }
                })
                .collect(Collectors.toCollection(ArrayList<Book>::new));
        return bookList;
    }

//
//    static int getIsbnFromSession(Session session) {
//        int isbn = session.attribute("isbnIndex");
//        return isbn;
//    }

}
