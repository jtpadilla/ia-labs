package io.github.jtpadilla.example.javelit.helloworld;

import io.javelit.core.Server;
import io.javelit.core.Jt;

class HelloWorld {

    static void main(String[] args) {
        final var server = Server.builder(HelloWorld::new, 8080).build();
        server.start();
    }

    public HelloWorld() {
        Jt.text("Hello World").use();
    }

}