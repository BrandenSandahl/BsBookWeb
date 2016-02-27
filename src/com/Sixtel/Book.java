package com.Sixtel;

/**
 * Created by branden on 2/25/16 at 12:32.
 */
public class Book {

    private String title, author, shortDescription;
    private int isbn, year;
    private char rating;
    private User owner;
    String link;

    //private final char[] RATINGS = {'A', 'B', 'C', 'D'};


    //constructors
    public Book() {
    }

    public Book(String title, String author, String shortDescription, int isbn, int year, char rating, User owner) {
        this.title = title;
        this.author = author;
        this.shortDescription = shortDescription;
        this.isbn = isbn;
        this.year = year;
        this.rating = rating;
        this.owner = owner;
    }


    //getters&setters with some custom functionality to get the user input
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getIsbn() {
        return isbn;
    }

    public void setIsbn(int isbn) {
        this.isbn = isbn;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public char getRating() {
        return rating;
    }

    public void setRating(char rating) {
        System.out.println("Enter the books rating");
//        // display the ratings real quick-like
//        for (int i = 0; i < RATINGS.length; i++) {
//            System.out.printf("%d.) %s%n", (i + 1), RATINGS[i]);
//        }
//        this.rating = RATINGS[(Integer.parseInt(BookStore.scanner.nextLine()) -1)];

        this.rating = rating;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String name) {
        this.link = (name.equals(this.getOwner().getName())) ? "<a href=\"/edit?isbnIndex=" + this.isbn + "\">Edit</a><br /><form action=\"delete\" method=\"post\"><a href=\"/?isbnIndex=" + this.isbn + "\">Delete</a></form>" : "" ;
    }

    //easy way to display input
    @Override
    public String toString() {
        return "Book{" +
                "author='" + author + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", isbn=" + isbn +
                ", year=" + year +
                ", rating=" + rating +
                '}';
    }


}