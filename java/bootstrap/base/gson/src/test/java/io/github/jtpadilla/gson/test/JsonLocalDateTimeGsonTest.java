package io.github.jtpadilla.gson.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.github.jtpadilla.gson.JsonLocalDateTimeAdapter;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class JsonLocalDateTimeGsonTest {

    final static private String JSON = "{local_date_time=\"2023-12-31T23:59:59\", description=\"La descripcion\"}";

    final static private String LOCAL_DATE_TIME = "local_date_time";
    final static private String DESCRIPTION = "description";

    static class SamplePojo {

        @SerializedName(LOCAL_DATE_TIME)
        LocalDateTime localDateTime;

        @SerializedName(DESCRIPTION)
        String name;

        public SamplePojo(LocalDateTime localDateTime, String name) {
            this.localDateTime = localDateTime;
            this.name = name;
        }

        public LocalDateTime localDateTime() {
            return localDateTime;
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SamplePojo that = (SamplePojo) o;
            return Objects.equals(localDateTime, that.localDateTime) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(localDateTime, name);
        }

    }

    private Gson gson;
    private SamplePojo samplePojo;

    @Before
    public void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new JsonLocalDateTimeAdapter())
                .create();
        LocalDateTime localDateTime = LocalDateTime.of(2023, Month.of(12), 31, 23, 59, 59);
        samplePojo = new SamplePojo(localDateTime, "La descripcion");
    }

    @Test
    public void testSerialize_ValidLocalDateTime_ReturnsJsonPrimitive() {
        // Given

        // When
        SamplePojo result = gson.fromJson(JSON, SamplePojo.class);

        // Then
        assertEquals(samplePojo, result);
    }

}
