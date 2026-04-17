package io.github.jtpadilla.example.langchain4j.agenticpure1.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.HashMap;
import java.util.Map;

public class BankTool {

    private static final String CTX = "El banco opera exclusivamente en euros (EUR). ";

    private final Map<String, Double> accounts = new HashMap<>();

    public void createAccount(String user, Double initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("Account for user " + user + " already exists");
        }
        accounts.put(user, initialBalance);
    }

    @Tool(CTX + "Obtiene el saldo actual de la cuenta del usuario indicado")
    public Double getBalance(@P("nombre de usuario") String user) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        return balance;
    }

    @Tool(CTX + "Lista todas las cuentas con sus saldos actuales como cadena de texto formateada")
    public String listAccounts() {
        if (accounts.isEmpty()) {
            return "No accounts found.";
        }
        StringBuilder sb = new StringBuilder();
        accounts.forEach((user, balance) ->
                sb.append(user).append(": ").append(balance).append("\n"));
        return sb.toString();
    }

    @Tool(CTX + "Abona al usuario indicado la cantidad especificada y devuelve el nuevo saldo")
    Double credit(@P("nombre de usuario") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance + amount;
        accounts.put(user, newBalance);
        return newBalance;
    }

    @Tool(CTX + "Retira la cantidad indicada de la cuenta del usuario y devuelve el nuevo saldo")
    Double withdraw(@P("nombre de usuario") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance - amount;
        accounts.put(user, newBalance);
        return newBalance;
    }

}