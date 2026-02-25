package io.github.jtpadilla.example.vertex.function.example01.model;

public class Address {
    public String street;
    public String city;
    public String state;
    public String zipCode;

    public Address() {}

    public Address(String street, String city, String state, String zipCode) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }
}