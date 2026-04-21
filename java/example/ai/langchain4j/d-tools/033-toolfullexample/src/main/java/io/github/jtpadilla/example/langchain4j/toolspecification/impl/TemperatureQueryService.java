package io.github.jtpadilla.example.langchain4j.toolspecification.impl;

import dev.langchain4j.model.chat.ChatModel;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityDataListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.schema.CityListSchema;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.FilterAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesAgent;
import io.github.jtpadilla.example.langchain4j.toolspecification.service.QueryCitiesDataAgent;

import java.util.ArrayList;
import java.util.List;

public class TemperatureQueryService {

    static public TemperatureQueryResult query(ChatModel chatModel, String provincia, List<String> ciudades) throws TemperatureQueryException {
        try {
            // Obtenemos la lista de ciudades con mas poblacion de la provincia proporcionada como parámetro
            CityListSchema cityList = QueryCitiesAgent.call(chatModel, provincia);
            System.out.println("[Lista de ciudades]");
            System.out.println(cityList.toJson());

            // Para cada una de las ciudades obtenemos la prevision de temperaturas
            final List<CityDataListSchema> dataList = new ArrayList<>();
            for (String cityName : cityList.getList()) {
                CityDataListSchema cityDataList = QueryCitiesDataAgent.call(chatModel, cityName);
                dataList.add(cityDataList);
                System.out.format("[Temperaturas para %s%n]", cityName);
                System.out.println(cityDataList.toJson());
            }

            // Finalmente, filtramos las lecturas que sean de determinadas ciudades y
            // únicamente la más alta para cada ciudad.
            final CityDataListSchema result = FilterAgent.call(chatModel, dataList, ciudades);

            // Se convierte el resultado
            return new TemperatureQueryResult(
                    result.getList().stream()
                            .map(entry -> new TemperatureEntry(entry.localDateTime(), entry.city(), entry.temperature()))
                            .toList()
            );
        } catch (RuntimeException e) {
            throw new TemperatureQueryException(e.getMessage(), e);
        }
    }


}
