package com.Sixtel;

import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;

public class Main {

    static HashMap<String, User> userMap = new HashMap<>(); //static map to hold all of our user accounts
    static HashMap<Integer, Book> bookMap = new HashMap<>(); //static map to hold all the books in our inventory
    static boolean passMisMatch = false; //this controls an HTML div. maybe look into better way to do this

    public static void main(String[] args) {
        Spark.externalStaticFileLocation("public");
        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    User user = getUserFromSession(session);

                    HashMap m = new HashMap();

                    //login a user logic
                    if (user != null) {
                        m.put("user", user);  //if the user has entered something then add to my hashmap
                        m.put("passMisMatch", passMisMatch); //i think this is just a way for mustache to display all this?
                        return new ModelAndView(m, "home.html");  //can also just pass the user here
                    } else {
                        m.put("passMisMatch", passMisMatch);
                        return new ModelAndView(m, "home.html");  //can also just pass the user here
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
                    //lets just grab all the fields first off. worry about error checking later yo.

                    String title = request.queryParams("bookTitleInput");
                    String author = request.queryParams("bookAuthorInput");
                    String description = request.queryParams("bookDescriptionInput");
                    char rating = request.queryParams("bookRatingInput").charAt(0);
                    int year = Integer.parseInt(request.queryParams("bookYearInput"));
                    int isbn = Integer.parseInt(request.queryParams("bookIsbnInput"));


                    //can we make an object?
                    Book b = new Book(title, author, description, isbn, year, rating);
                    //can we add it? ... .. .... Who is we? Hello?
                    bookMap.put(b.getIsbn(), b);
                    return "";
                })
        );

    }

    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return userMap.get(name);


    }


}
