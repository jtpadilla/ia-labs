package io.github.jtpadilla.example.vertex.function.example01;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.Type;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.github.jtpadilla.example.vertex.function.example01.model.Address;
import io.github.jtpadilla.example.vertex.function.example01.model.ContactInfo;
import io.github.jtpadilla.example.vertex.function.example01.model.Customer;
import io.github.jtpadilla.example.vertex.function.example01.model.Order;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class FunctionDefinitions {

    private final Tool tool;
    private final Map<String, Method> methodMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static FunctionDefinitions instance;

    private FunctionDefinitions() throws NoSuchMethodException {
        // Lista de métodos que se expondrán como funciones
        Method[] methods = new Method[]{
                getClass().getDeclaredMethod("getCustomer", int.class),
                getClass().getDeclaredMethod("createCustomer", String.class, String.class, String.class, String.class, String.class, String.class, String.class),
                getClass().getDeclaredMethod("createOrder", int.class, String[].class, double.class)
        };

        // Añadir metodos a un mapa para su facil acceso. Se usa en el metodo invokeFunction
        for (Method method : methods) {
            methodMap.put(method.getName(), method);
        }

        // Crear la definición de funciones y la herramienta
        List<FunctionDeclaration> functionDeclarations = createFunctionDeclarations(methods);
        this.tool = Tool.newBuilder()
                .addAllFunctionDeclarations(functionDeclarations)
                .build();
    }

    public static synchronized FunctionDefinitions getInstance() throws NoSuchMethodException {
        if (instance == null) {
            instance = new FunctionDefinitions();
        }
        return instance;
    }

    public Tool getTool() {
        return tool;
    }

    // Simula una base de datos en memoria para el ejemplo
    private final Map<Integer, Customer> customerDatabase = Map.of(
            123, new Customer("Alice Smith", 123, new ContactInfo("alice@example.com", "555-1234", new Address("123 Main St", "Anytown", "CA", "12345"))),
            456, new Customer("Bob Johnson", 456, new ContactInfo("bob@example.com", "555-5678", new Address("456 Oak Ave", "Springfield", "IL", "67890")))
    );
    private final Map<Integer, Order> orderDatabase = new HashMap<>();

    // Funciones a ser expuestas a Vertex AI (sin la anotación @Function)

    public String getCustomer(int customerId) throws IOException {
        Customer customer = customerDatabase.get(customerId);
        if (customer == null) {
            return String.format("{\"error\": \"Customer with ID %d not found\"}", customerId);
        }
        return objectMapper.writeValueAsString(customer);
    }

    public String createCustomer(String name, String email, String phoneNumber, String street, String city, String state, String zipCode) throws IOException {
        int customerId = customerDatabase.size() + 1;
        Address address = new Address(street, city, state, zipCode);
        ContactInfo contactInfo = new ContactInfo(email, phoneNumber, address);
        Customer customer = new Customer(name, customerId, contactInfo);
        customerDatabase.put(customerId, customer);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("customerId", customerId);
        return objectMapper.writeValueAsString(result);
    }

    public String createOrder(int customerId, String[] items, double totalPrice) throws IOException {
        Customer customer = customerDatabase.get(customerId);
        if (customer == null) {
            return String.format("{\"error\": \"Customer with ID %d not found\"}", customerId);
        }
        int orderId = orderDatabase.size() + 1;
        Order order = new Order(orderId, customer, List.of(items), totalPrice);
        orderDatabase.put(orderId, order);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("orderId", orderId);
        return objectMapper.writeValueAsString(result);
    }

    public String invokeFunction(String functionName, Map<String, Value> args) {
        try {
            Method method = methodMap.get(functionName);
            if (method == null) {
                return String.format("{\"error\": \"Function %s not found\"}", functionName);
            }

            // Convertir los argumentos al tipo correcto
            Object[] typedArgs = convertArgs(method, args);

            // Invocar el método y devolver el resultado como JSON
            Object result = method.invoke(this, typedArgs);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            return String.format("{\"error\": \"Error invoking function %s: %s\"}", functionName, e.getMessage());
        }
    }

    private Object[] convertArgs(Method method, Map<String, Value> args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] typedArgs = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String paramName = method.getParameters()[i].getName();
            Value argValue = args.get(paramName);

            if (argValue != null) {
                if (parameterTypes[i] == int.class) {
                    typedArgs[i] = (int) argValue.getNumberValue();
                } else if (parameterTypes[i] == String.class) {
                    typedArgs[i] = argValue.getStringValue();
                } else if (parameterTypes[i] == double.class) {
                    typedArgs[i] = argValue.getNumberValue();
                } else if (parameterTypes[i] == String[].class) {
                    ListValue listValue = argValue.getListValue();
                    String[] stringArray = new String[listValue.getValuesCount()];
                    for (int j = 0; j < listValue.getValuesCount(); j++) {
                        stringArray[j] = listValue.getValues(j).getStringValue();
                    }
                    typedArgs[i] = stringArray;
                } else {
                    typedArgs[i] = argValue;
                }
            }
        }

        return typedArgs;
    }

    // Metodos para la creacion de los schemas y FunctionDeclaration

    private List<FunctionDeclaration> createFunctionDeclarations(Method[] methods) {
        List<FunctionDeclaration> functionDeclarations = new ArrayList<>();
        for (Method method : methods) {
            functionDeclarations.add(createFunctionDeclaration(method));
        }
        return functionDeclarations;
    }

    private FunctionDeclaration createFunctionDeclaration(Method method) {
        FunctionDeclaration.Builder builder = FunctionDeclaration.newBuilder()
                .setName(method.getName())
                .setDescription(createDescription(method)); // Implementa la lógica para crear descripciones

        Schema.Builder parametersSchema = Schema.newBuilder().setType(Type.OBJECT);
        Arrays.stream(method.getParameters()).forEach(param -> {
            parametersSchema.putProperties(param.getName(), createPropertySchema(param.getType()));
        });

        builder.setParameters(parametersSchema);
        return builder.build();
    }

    private Schema createPropertySchema(Class<?> type) {
        Schema.Builder builder = Schema.newBuilder();

        if (type == int.class || type == Integer.class) {
            builder.setType(Type.INTEGER);
        } else if (type == String.class) {
            builder.setType(Type.STRING);
        } else if (type == double.class || type == Double.class) {
            builder.setType(Type.NUMBER);
        } else if (type == boolean.class || type == Boolean.class) {
            builder.setType(Type.BOOLEAN);
        } else if (type == String[].class) {
            builder.setType(Type.ARRAY);
            builder.setItems(Schema.newBuilder().setType(Type.STRING).build());
        } else {
            // Para tipos complejos, crea una referencia al esquema del tipo
            builder.setType(Type.OBJECT);
        }
        return builder.build();
    }

    private String createDescription(Method method) {
        switch (method.getName()) {
            case "getCustomer":
                return "Obtiene la información de un cliente por su ID.";
            case "createCustomer":
                return "Crea un nuevo cliente. Devuelve el ID del cliente creado.";
            case "createOrder":
                return "Crea una nueva orden para un cliente. Devuelve el ID de la orden creada.";
            default:
                return "Descripción no disponible.";
        }
    }

}
