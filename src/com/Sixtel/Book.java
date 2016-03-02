package com.Sixtel;

/**
 * Created by branden on 2/25/16 at 12:32.
 */
public class Book implements Comparable {

    private String title, author, description;
    private Integer isbn;
    private Integer year;
    private Integer bookId;
    private Character rating;
    private String ownerName;
    boolean isOwner;


    //private final char[] RATINGS = {'A', 'B', 'C', 'D'};


    //constructors
    public Book() {
    }


    public Book(String title, String author, String description, int isbn, int year, char rating, String ownerName) {
        setTitle(title);
        setAuthor(author);
        setDescription(description);
        setIsbn(isbn);
        setYear(year);
        setRating(rating);
        setOwnerName(ownerName);
    }


    //getters&setters with some custom functionality to get the user input
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getIsbn() {
        return isbn;
    }

    public void setIsbn(Integer isbn) {
        this.isbn = isbn;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Character getRating() {
        return rating;
    }

    public void setRating(Character rating) {
        System.out.println("Enter the books rating");
//        // display the ratings real quick-like
//        for (int i = 0; i < RATINGS.length; i++) {
//            System.out.printf("%d.) %s%n", (i + 1), RATINGS[i]);
//        }
//        this.rating = RATINGS[(Integer.parseInt(BookStore.scanner.nextLine()) -1)];

        this.rating = rating;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }


    public boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(boolean owner) {
        isOwner = owner;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }


    @Override
    public int compareTo(Object o) {
        Book b = (Book) o;
        return  (!author.equalsIgnoreCase(b.author))  ? author.compareTo(b.author) : title.compareTo(b.title);
    }




    //easy way to display input
    @Override
    public String toString() {
        return "Book{" +
                "author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", isbn=" + isbn +
                ", year=" + year +
                ", rating=" + rating +
                '}';
    }


}