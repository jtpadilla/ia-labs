package io.github.jtpadilla.example.langchain4j.toolspecification;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class WeatherTool {

    private static final Gson GSON = new Gson();

    // Simulated tool execution: parses the JSON arguments and returns a fake forecast
    public static String execute(String argumentsJson) {
        JsonObject args = GSON.fromJson(argumentsJson, JsonObject.class);
        String city = args.has("city") ? args.get("city").getAsString() : "Unknown";
        String unit = args.has("temperatureUnit") ? args.get("temperatureUnit").getAsString() : "CELSIUS";
        String temp = unit.equals("FAHRENHEIT") ? "59°F" : "15°C";
        return "Tomorrow in " + city + ": cloudy with light rain, " + temp + ".";
    }

}
