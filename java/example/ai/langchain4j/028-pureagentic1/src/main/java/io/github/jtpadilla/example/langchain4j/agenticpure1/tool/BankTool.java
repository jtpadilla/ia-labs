package io.github.jtpadilla.example.langchain4j.agenticpure1.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.HashMap;
import java.util.Map;

public class BankTool {

    private final Map<String, Double> accounts = new HashMap<>();

    public void createAccount(String user, Double initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("Account for user " + user + " already exists");
        }
        accounts.put(user, initialBalance);
    }

    @Tool("Get the current balance of the given user account")
    public Double getBalance(@P("user name") String user) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        return balance;
    }

    @Tool("List all accounts with their current balances as a formatted string")
    public String listAccounts() {
        if (accounts.isEmpty()) {
            return "No accounts found.";
        }
        StringBuilder sb = new StringBuilder();
        accounts.forEach((user, balance) ->
                sb.append(user).append(": ").append(balance).append("\n"));
        return sb.toString();
    }

    @Tool("Credit the given user with the given amount and return the new balance")
    Double credit(@P("user name") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance + amount;
        accounts.put(user, newBalance);
        return newBalance;
    }

    @Tool("Withdraw the given amount with the given user and return the new balance")
    Double withdraw(@P("user name") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance - amount;
        accounts.put(user, newBalance);
        return newBalance;
    }

}