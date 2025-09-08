package com.example.akkubattrent.Classes;

public class User {
    private int id;
    private String phoneNumber;
    private String name;

    public User(int id, String phoneNumber, String name) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    public int getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getName() { return name; }
}