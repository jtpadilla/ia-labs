package io.github.jtpadilla.example.javelit.pages;

import io.javelit.core.Jt;
import io.javelit.core.Server;

class Pages {

    static void main(String[] args) {
        Server.builder(Pages::javelit, 8080)
                .build()
                .start();
    }

    static private void javelit() {

        // register the pages
        var currentPage = Jt.navigation(
                Jt.page("/main", Pages::home).title("Home").icon("🎈"),
                Jt.page("/page2", Pages::page2).title("Page 2").icon("❄️"),
                Jt.page("/page3", Pages::page3).title("Page 3").icon("🎉")
        ).use();

        // run the current page
        currentPage.run();

    }

    public static void home() {
        Jt.title("Main Page 🎈").use();
        Jt.markdown("Welcome to the main page!").use();
    }

    public static void page2() {
        Jt.title("Page 2 ❄️").use();
        Jt.text("This is the second page").use();
    }

    public static void page3() {
        Jt.title("Page 3 🎉").use();
        Jt.markdown("This is the **third** page!").use();
    }

}