package io.github.jtpadilla.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class GsonUtil {

    static public class Builder {

        @Nullable
        private JsonInstantAdapter instantAdapter;
        @Nullable
        private JsonLocalDateTimeAdapter localDateTimeAdapter;
        @Nullable
        private JsonZonedDateTimeAdapter zonedDateTimeAdapter;

        public Builder instantAdapter(@Nullable JsonInstantAdapter instantAdapter) {
            this.instantAdapter = instantAdapter;
            return this;
        }

        public Builder localDateTimeAdapter(@Nullable JsonLocalDateTimeAdapter localDateTimeAdapter) {
            this.localDateTimeAdapter = localDateTimeAdapter;
            return this;
        }

        public Builder zonedDateTimeAdapter(@Nullable JsonZonedDateTimeAdapter zonedDateTimeAdapter) {
            this.zonedDateTimeAdapter = zonedDateTimeAdapter;
            return this;
        }

        public Gson build() {
            GsonBuilder gsonBuilder = new GsonBuilder();
            if (instantAdapter != null) {
                gsonBuilder.registerTypeAdapter(Instant.class, instantAdapter);
            }
            if (localDateTimeAdapter != null) {
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, localDateTimeAdapter);
            }
            if (zonedDateTimeAdapter != null) {
                gsonBuilder.registerTypeAdapter(ZonedDateTime.class, zonedDateTimeAdapter);
            }
            return gsonBuilder.create();
        }

        public Gson buildAll() {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Instant.class, instantAdapter);
            gsonBuilder.registerTypeAdapter(LocalDateTime.class, localDateTimeAdapter);
            gsonBuilder.registerTypeAdapter(ZonedDateTime.class, zonedDateTimeAdapter);
            return gsonBuilder.create();
        }

    }

    static public Builder builder() {
        return new Builder();
    }

}
