package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;


import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {

    static HashMap<String, User> userMap = new HashMap<>(); //static map to hold all of our user accounts
    static  HashMap<Integer, Book> bookMap = new HashMap<>(); //need to be able to access our books by an ID
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
            return b;
        }
        return null;
    }









    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Spark.externalStaticFileLocation("public"); //link to location of CSS files

        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    boolean haveBook = !bookMap.isEmpty();  //hides the book table if there are no items in it


                    HashMap m = new HashMap();
                    m.put("passMisMatch", passMisMatch);  //not really used right now, will try to implement later
                    m.put("haveBook", haveBook);

                    //Array for mustache display purposes
                    ArrayList<Book> bookListEditable = new ArrayList<>();
                    for (Book b : bookMap.values()) {
                        //can set an index value in here bases on sizeOf method
                        bookListEditable.add(b);
                    }

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
                    bookListEditable = getBookOwnership(user, bookListEditable);
                    m.put("bookList", bookListEditable.subList(subStart, subTo));
                    //next and prev anchors
                    m.put("next", ((subTo != (bookListEditable.size())) ? subStart + 5 : null));  //adjust next link, hide if the end of my subList is at the end of the ArrayList
                    m.put("previous", (subStart != 0) ? subStart - 5 : null); //adjust previous link, hide if it's at 0.


                    if (user != null) {
                        m.put("user", user); //link to the user
                        m.put("userName", user.getUserName());  //get the name, this is a little redundant right now, need to work on
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
                   // User user = getUserFromSession(request1.session());
                    int isbnIndex = Integer.valueOf(request1.queryParams("isbnIndex"));

                    Book b = bookMap.get(isbnIndex);

                    HashMap m = new HashMap();
                    m.put("book", b); //place book in the map so we can populate a page with the books info.
                    return new ModelAndView(m, "view.html");
                }),
                new MustacheTemplateEngine()
        );


        Spark.get(
                "/edit",
                ((request1, response1) -> {
                    User user = getUserFromSession(request1.session());
                        if (user == null) {
                            throw  new  Exception("Someone is trying to edit a book without authorization. They are not authorized. There is no authorization.");
                    }
                    //get the book object the user clicked on
                    int isbnIndex = Integer.valueOf(request1.queryParams("isbnIndex"));
                    Session session = request1.session();
                    session.attribute("isbnIndex", isbnIndex);  //add book into session

                    Book b = bookMap.get(isbnIndex);

                    HashMap m = new HashMap();

                    m.put("book", b); //place book in the map so we can populate a page with the books info.
                    return new ModelAndView(m, "edit.html");
                }),
        new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                ((request, response) -> {
                    String name = request.queryParams("nameInput");
                    String pass = request.queryParams("passwordInput");


                    if (!userMap.containsKey(name)) {  //if the user does not yet exist
                        //add the user to the map
                        userMap.put(name, new User(name, pass));
                        //create session for user
                        Session session = request.session();
                        session.attribute("userName", name);

                        response.redirect("/");
                        return "";
                    } else if ((userMap.get(name).getUserPassword().equalsIgnoreCase(pass))) {   //if the user does exist and pass matches
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
                    User user = getUserFromSession(request.session());

                    //lets just grab all the fields first off. worry about error checking later yo.
                    String title = request.queryParams("bookTitleInput");
                    String author = request.queryParams("bookAuthorInput");
                    String description = request.queryParams("bookDescriptionInput");
                    char rating = request.queryParams("bookRatingInput").charAt(0);
                    int year = Integer.parseInt(request.queryParams("bookYearInput"));
                    //for some reason i could not do this all in one go ??
                    String isbnString = request.queryParams("bookIsbnInput");
                    int isbn = Integer.parseInt(isbnString);


                    //can we make an object?
                    Book b = new Book(title, author, description, isbn, year, rating, user);
                    //can we add it? ... .. .... Who is we? Hello?
                    bookMap.put(isbn, b);
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
                    User user = getUserFromSession(request.session());

                    //this could be sent in a hidden Input field
                    int isbnIndex = getIsbnFromSession(request.session());


                    //get all my fields
                    String title = request.queryParams("bookTitleInput");
                    String author = request.queryParams("bookAuthorInput");
                    String description = request.queryParams("bookDescriptionInput");
                    char rating = request.queryParams("bookRatingInput").charAt(0);
                    int year = Integer.parseInt(request.queryParams("bookYearInput"));
                    //for some reason i could not do this all in one go ??
                    String isbnString = request.queryParams("bookIsbnInput");
                    int isbn = Integer.parseInt(isbnString);

                    Book bookEdited = new Book(title, author, description, isbn, year, rating, user);


                    bookMap.remove(isbnIndex); //have to do this in case we changed the ISBN
                    bookMap.put(isbn, bookEdited); //this should update the map


                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/delete",
                ((request1, response1) -> {
                    int isbnIndex = Integer.parseInt(request1.queryParams("isbnIndex"));

                    bookMap.remove(isbnIndex);

                    response1.redirect("/");
                    return "";
                })
        );
    }

    //stream function that will set an editable field within Book
    //this runs via mustaches interaction behavior with an ArrayList
    static ArrayList<Book> getBookOwnership(User u, ArrayList<Book> bookList) {
        bookList = bookList.stream()
                .map((book) -> {
                    if (u != null) {
                       // book.setOwner(u.getUserName().equals(book.getOwner().getUserName()));
                        return book;
                    } else {
                        book.setOwner(false);
                        return book;
                    }
                })
                .collect(Collectors.toCollection(ArrayList<Book>::new));
        return bookList;
    }

    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return userMap.get(name);
    }

    static int getIsbnFromSession(Session session) {
        int isbn = session.attribute("isbnIndex");
        return isbn;
    }

}
