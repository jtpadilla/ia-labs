package io.github.jtpadilla.example.telegram.config;

import io.github.jtpadilla.example.telegram.UnitResources;
import io.github.jtpadilla.unit.parameter.IParameterDescriptor;
import io.github.jtpadilla.unit.parameter.IParameterReader;
import io.github.jtpadilla.unit.parameter.IParameterReaderBuilder;

public class TelegramBotConfig {

	static private IParameterDescriptor TELEGRAM_BOT_EXAMPLE_APIKEY = IParameterDescriptor.createStringParameterDescriptor(
			"TELEGRAM_BOT_EXAMPLE_APIKEY",
			"ApiKey del bot"
	);


    static public TelegramBotConfig getConfig() {
        return new TelegramBotConfig();
    }

    final private String apiKey;

    private TelegramBotConfig() {
        IParameterReader reader = IParameterReaderBuilder.create(UnitResources.UNIT)
                .addParameterDescriptor(TELEGRAM_BOT_EXAMPLE_APIKEY)
                .build();
        apiKey = reader.getValue(TELEGRAM_BOT_EXAMPLE_APIKEY).getStringValue();
    }

    public String getApiKey() {
        return apiKey;
    }

}
