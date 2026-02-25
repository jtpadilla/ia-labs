package io.github.jtpadilla.example.vertex.function.example01.model;

public class ContactInfo {
    public String email;
    public String phoneNumber;
    public Address address;

    public ContactInfo() {}

    public ContactInfo(String email, String phoneNumber, Address address) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
}