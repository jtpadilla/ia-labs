package io.github.jtpadilla.example.javelit.displaytablelist;

import io.javelit.core.Jt;
import io.javelit.core.Server;

import java.util.List;

class DisplayTableList {

    static void main(String[] args) {
        Server.builder(DisplayTableList::javelit, 8080)
                .build()
                .start();

    }

    static private void javelit() {

        record Person(String name, int age, String city) {}

        List<Person> people = List.of(
                new Person("Alice", 30, "NYC"),
                new Person("Bob", 25, "LA")
        );

        Jt.table(people).use();

    }

}