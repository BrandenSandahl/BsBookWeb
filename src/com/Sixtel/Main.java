package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {

    static HashMap<String, User> userMap = new HashMap<>(); //static map to hold all of our user accounts
    static ArrayList<Book> bookList =  new ArrayList<>(); //static list to hold all books in our inventory
    static boolean passMisMatch = false; //this controls an HTML div. maybe look into better way to do this


    public static void main(String[] args) {
        Spark.externalStaticFileLocation("public"); //link to location of CSS files
        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    User user = getUserFromSession(session);
                //    boolean haveBook = !bookList.isEmpty();  //not used right now, will put this back in later

                    HashMap m = new HashMap();
                    m.put("passMisMatch", passMisMatch);  //not really used right now, will try to implement later
           //         m.put("haveBook", haveBook);

                    ArrayList<Book> bookListEditable = getBookOwnership(user, bookList); //need to run my function to turn on and off editable fields based on login status
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
                    bookList.add(b);
                 //   bookMap.put(b.getIsbn(), b);
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
    }


    //stream reader function that will set an editable field within Book
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


}
