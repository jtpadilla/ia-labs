package io.github.jtpadilla.example.vertex.function.example01.model;

import java.util.List;

public class Order {
    public int orderId;
    public Customer customer;
    public List<String> items;
    public double totalPrice;

    public Order() {}

    public Order(int orderId, Customer customer, List<String> items, double totalPrice) {
        this.orderId = orderId;
        this.customer = customer;
        this.items = items;
        this.totalPrice = totalPrice;
    }

}