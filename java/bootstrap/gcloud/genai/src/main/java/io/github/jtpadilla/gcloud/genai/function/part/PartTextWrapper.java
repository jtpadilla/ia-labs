package io.github.jtpadilla.gcloud.genai.function.part;

import com.google.genai.types.Part;

final public class PartTextWrapper extends PartWrapper {

    final private String text;

    public PartTextWrapper(Part part, String text) {
        super(part);
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "PartTextWrapper{" +
                "text='" + text + '\'' +
                '}';
    }

}
