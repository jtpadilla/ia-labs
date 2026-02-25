package com.speedycontrol.labs.example.vertextai.function.example01.model;

public class Customer {
    public String name;
    public int customerId;
    public ContactInfo contactInfo;

    public Customer() {}

    public Customer(String name, int customerId, ContactInfo contactInfo) {
        this.name = name;
        this.customerId = customerId;
        this.contactInfo = contactInfo;
    }
}