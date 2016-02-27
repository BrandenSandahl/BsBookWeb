package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {

    static HashMap<String, User> userMap = new HashMap<>(); //static map to hold all of our user accounts
    static  HashMap<Integer, Book> bookMap = new HashMap<>(); //need to be able to access our books by an ID
    static boolean passMisMatch = false; //this controls an HTML div. maybe look into better way to do this


    public static void main(String[] args) {
        Spark.externalStaticFileLocation("public"); //link to location of CSS files
        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    boolean haveBook = !bookMap.isEmpty();  //not used right now, will put this back in later

                    HashMap m = new HashMap();
                    m.put("passMisMatch", passMisMatch);  //not really used right now, will try to implement later
                    m.put("haveBook", haveBook);

                    //Array for mustache display purposes
                    ArrayList<Book> bookListEditable = new ArrayList<>();
                    for (Book b : bookMap.values()) {
                        bookListEditable.add(b);
                    }
                    //run through a method that checks to see if logged in user is the owner of the book, and allows editing and deletion.
                    bookListEditable = getBookOwnership(user, bookListEditable);
                    m.put("bookList", bookListEditable);

                    if (user != null) {
                        m.put("user", user); //link to the user
                        m.put("userName", user.getName());  //get the name, this is a little redundant right now, need to work on
                        return new ModelAndView(m, "home.html");
                    } else {
                        return new ModelAndView(m, "home.html");
                    }
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
                    } else if ((userMap.get(name).getPassword().equalsIgnoreCase(pass))) {   //if the user does exist and pass matches
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
                    int isbnIndex =getIsbnFromSession(request1.session());

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
                        book.setLink(u.getName());
                        return book;
                    } else {
                        book.setLink("");
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
