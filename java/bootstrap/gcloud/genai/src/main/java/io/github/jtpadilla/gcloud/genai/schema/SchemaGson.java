package io.github.jtpadilla.gcloud.genai.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jtpadilla.gson.JsonInstantAdapter;
import io.github.jtpadilla.gson.JsonLocalDateTimeAdapter;
import io.github.jtpadilla.gson.JsonZonedDateTimeAdapter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class SchemaGson {

    final static public Gson DEFAULT_GSON = createDefault();

    static Gson createDefault() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new JsonInstantAdapter())
                .registerTypeAdapter(LocalDateTime.class, new JsonLocalDateTimeAdapter())
                .registerTypeAdapter(ZonedDateTime.class, new JsonZonedDateTimeAdapter())
                .create();
    }

}
