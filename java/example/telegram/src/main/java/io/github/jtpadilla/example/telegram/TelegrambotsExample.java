package io.github.jtpadilla.example.telegram;

import io.github.jtpadilla.example.telegram.config.TelegramBotConfig;
import io.github.jtpadilla.example.telegram.impl.MyAmazingBot;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class TelegrambotsExample {

    static final String BOT_TOKEN = TelegramBotConfig.getConfig().getApiKey();

    public static void main(String[] args) {

        // Using try-with-resources to allow autoclose to run upon finishing
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(BOT_TOKEN, new MyAmazingBot(BOT_TOKEN));
            System.out.println("MyAmazingBot successfully started!");
            // Ensure this prcess wait forever
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
