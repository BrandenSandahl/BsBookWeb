package com.Sixtel;

/**
 * Created by branden on 2/25/16 at 17:54.
 */
public class User {

    private String userName, userPassword;
    int id; //match this up with the DB


    public User(String name, String userPassword) {
        setUserName(name);
        setUserPassword(userPassword);
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}