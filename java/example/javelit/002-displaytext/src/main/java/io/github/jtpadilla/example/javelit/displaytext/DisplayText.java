package io.github.jtpadilla.example.javelit.displaytext;

import io.javelit.core.Jt;
import io.javelit.core.Server;

class DisplayText {

    static void main(String[] args) {
        Server.builder(DisplayText::new, 8080)
                .build()
                .start();

    }

    public DisplayText() {
        Jt.title("My Javelit Example App").use();

        Jt.markdown("""
                    ## Welcome to Javelit!
                    This is a **markdown** example with:
                    - *Italic text*
                    - **Bold text**
                    - [A link](https://docs.javelit.io)
                    """).use();

        Jt.text("This is plain text displayed using Jt.text()").use();
        Jt.code("""
                print("hello world");
                """)
                .language("python").use();
    }

}