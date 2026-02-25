package io.github.jtpadilla.gcloud.genai.function.part;

import com.google.genai.types.Part;

final public class PartOtherWrapper extends PartWrapper {

    public PartOtherWrapper(Part part) {
        super(part);
    }

    @Override
    public String toString() {
        return "PartOtherWrapper{}";
    }

}

