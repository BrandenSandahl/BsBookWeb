package com.Sixtel;

import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import spark.ModelAndView;
import spark.*;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
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
                        m.put("userName", user.getName());  //get the name, this is a little redundant right now, need to work on
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
                        book.setOwner(u.getName().equals(book.getOwner().getName()));
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


//    static HashMap<String, User> readFromJson() throws FileNotFoundException {
//
//
//        JsonParser parser = new JsonParser();
//        File f = new File("microMessages.json");
//        Scanner scanner = new Scanner(f);
//
//        scanner.useDelimiter("\\Z");
//        String data = scanner.next();
//
//        JsonWrapper wrappedData = parser.parse(data, JsonWrapper.class);
//
//        HashMap<String, User> m = new HashMap<>();
//
//        wrappedData.getWrappedData().forEach((k, v) -> m.put(k, v));  //anon function that populates a map
//
//        return m;
//    }
//
//    static void saveToJson() throws IOException {
//        JsonSerializer serializer = new JsonSerializer();
//        File f = new File("microMessages.json");
//        FileWriter fw = new FileWriter(f);
//
//        JsonWrapper wrapper = new JsonWrapper(bookMap);  //special class made solely to wrap the hashmap containing users
//
//        //note this little call to setClassMetadataName here. Had to do that to make this work. From Docs.
//        String serialized = serializer.deep(true).include("*").serialize(wrapper);
//
//        fw.write(serialized);
//        fw.close();
//    }
//


}
