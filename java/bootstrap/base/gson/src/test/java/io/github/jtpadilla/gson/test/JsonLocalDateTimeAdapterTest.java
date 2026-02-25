package io.github.jtpadilla.gson.test;

import com.google.gson.*;
import io.github.jtpadilla.gson.JsonLocalDateTimeAdapter;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class JsonLocalDateTimeAdapterTest {

    private JsonLocalDateTimeAdapter adapter;
    private Type mockType;
    private JsonSerializationContext mockSerializationContext;
    private JsonDeserializationContext mockDeserializationContext;

    @Before
    public void setUp() {
        adapter = new JsonLocalDateTimeAdapter();
        mockType = mock(Type.class);
        mockSerializationContext = mock(JsonSerializationContext.class);
        mockDeserializationContext = mock(JsonDeserializationContext.class);
    }

    @Test
    public void testSerialize_ValidLocalDateTime_ReturnsJsonPrimitive() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
        String expectedString = "2023-12-25T14:30:45";

        // When
        JsonElement result = adapter.serialize(dateTime, mockType, mockSerializationContext);

        // Then
        assertNotNull(result);
        assertTrue(result.isJsonPrimitive());
        assertEquals(expectedString, result.getAsString());
    }

    @Test
    public void testSerialize_LocalDateTimeWithNanos_ReturnsFormattedString() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);
        String expectedString = "2023-12-25T14:30:45.123456789";

        // When
        JsonElement result = adapter.serialize(dateTime, mockType, mockSerializationContext);

        // Then
        assertEquals(expectedString, result.getAsString());
    }

    @Test
    public void testSerialize_EdgeCaseDates_ReturnsCorrectFormat() {
        // Given
        LocalDateTime minDate = LocalDateTime.MIN;
        LocalDateTime maxDate = LocalDateTime.MAX;

        // When
        JsonElement minResult = adapter.serialize(minDate, mockType, mockSerializationContext);
        JsonElement maxResult = adapter.serialize(maxDate, mockType, mockSerializationContext);

        // Then
        assertNotNull(minResult);
        assertNotNull(maxResult);
        assertTrue(minResult.isJsonPrimitive());
        assertTrue(maxResult.isJsonPrimitive());
    }

    @Test
    public void testDeserialize_ValidIsoString_ReturnsLocalDateTime() {
        // Given
        String dateTimeString = "2023-12-25T14:30:45";
        JsonElement jsonElement = new JsonPrimitive(dateTimeString);
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 14, 30, 45);

        // When
        LocalDateTime result = adapter.deserialize(jsonElement, mockType, mockDeserializationContext);

        // Then
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    public void testDeserialize_StringWithNanos_ReturnsLocalDateTimeWithNanos() {
        // Given
        String dateTimeString = "2023-12-25T14:30:45.123456789";
        JsonElement jsonElement = new JsonPrimitive(dateTimeString);
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);

        // When
        LocalDateTime result = adapter.deserialize(jsonElement, mockType, mockDeserializationContext);

        // Then
        assertEquals(expected, result);
    }

    @Test
    public void testDeserialize_StringWithSeconds_ReturnsCorrectLocalDateTime() {
        // Given
        String dateTimeString = "2023-12-25T14:30:45";
        JsonElement jsonElement = new JsonPrimitive(dateTimeString);

        // When
        LocalDateTime result = adapter.deserialize(jsonElement, mockType, mockDeserializationContext);

        // Then
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(45, result.getSecond());
        assertEquals(0, result.getNano());
    }

    @Test(expected = DateTimeParseException.class)
    public void testDeserialize_InvalidFormat_ThrowsJsonParseException() {
        // Given
        String invalidDateTimeString = "2023-12-25 14:30:45";
        JsonElement jsonElement = new JsonPrimitive(invalidDateTimeString);

        // When & Then
        adapter.deserialize(jsonElement, mockType, mockDeserializationContext);
    }

    @Test(expected = DateTimeParseException.class)
    public void testDeserialize_EmptyString_ThrowsJsonParseException() {
        // Given
        JsonElement jsonElement = new JsonPrimitive("");

        // When & Then
        adapter.deserialize(jsonElement, mockType, mockDeserializationContext);
    }

    @Test(expected = NullPointerException.class)
    public void testDeserialize_NullJsonElement_ThrowsException() {
        // Given
        JsonElement jsonElement = null;

        // When & Then
        adapter.deserialize(jsonElement, mockType, mockDeserializationContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeserialize_JsonNull_ThrowsException() {
        // Given
        JsonElement jsonElement = JsonNull.INSTANCE;

        // When & Then
        adapter.deserialize(jsonElement, mockType, mockDeserializationContext);
    }

    @Test
    public void testRoundTrip_SerializeAndDeserialize_ReturnsOriginalValue() {
        // Given
        LocalDateTime original = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);

        // When
        JsonElement serialized = adapter.serialize(original, mockType, mockSerializationContext);
        LocalDateTime deserialized = adapter.deserialize(serialized, mockType, mockDeserializationContext);

        // Then
        assertEquals(original, deserialized);
    }

    @Test
    public void testSerializeDeserialize_Midnight_HandlesCorrectly() {
        // Given
        LocalDateTime midnight = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        // When
        JsonElement serialized = adapter.serialize(midnight, mockType, mockSerializationContext);
        LocalDateTime deserialized = adapter.deserialize(serialized, mockType, mockDeserializationContext);

        // Then
        assertEquals(midnight, deserialized);
        assertEquals("2023-01-01T00:00:00", serialized.getAsString());
    }

    @Test
    public void testSerializeDeserialize_EndOfYear_HandlesCorrectly() {
        // Given
        LocalDateTime endOfYear = LocalDateTime.of(2023, 12, 31, 23, 59, 59);

        // When
        JsonElement serialized = adapter.serialize(endOfYear, mockType, mockSerializationContext);
        LocalDateTime deserialized = adapter.deserialize(serialized, mockType, mockDeserializationContext);

        // Then
        assertEquals(endOfYear, deserialized);
        assertEquals("2023-12-31T23:59:59", serialized.getAsString());
    }

}
