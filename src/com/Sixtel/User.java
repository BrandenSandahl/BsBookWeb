package com.Sixtel;

/**
 * Created by branden on 2/25/16 at 17:54.
 */
public class User {

    private String name, password;


    public User(String name, String password) {
        setName(name);
        setPassword(password);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}