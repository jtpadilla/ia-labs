package io.github.jtpadilla.study.xodus.env;

import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.StoreConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public class EnvironmentMain {

    public static void main(String[] args) throws Exception {
        LogConfig.configureRuntime();

        Path dbPath = Files.createTempDirectory("xodus-env-");
        System.out.println("Base de datos Xodus Environment en: " + dbPath);

        var env = Environments.newInstance(dbPath.toFile());

        WebServer server = WebServer.builder()
                .port(8084)
                .routing(routing -> routing
                        .get("/xodus/env", (req, res) -> {
                            env.executeInTransaction(txn -> {
                                var store = env.openStore("TestStore", StoreConfig.WITHOUT_DUPLICATES, txn);
                                store.put(txn, 
                                    StringBinding.stringToEntry("key1"), 
                                    StringBinding.stringToEntry("Hola Xodus Env"));
                            });
                            
                            String val = env.computeInReadonlyTransaction(txn -> {
                                var store = env.openStore("TestStore", StoreConfig.WITHOUT_DUPLICATES, txn);
                                var entry = store.get(txn, StringBinding.stringToEntry("key1"));
                                return entry != null ? StringBinding.entryToString(entry) : "No encontrado";
                            });
                            res.send("KV Store Layer: " + val);
                        })
                )
                .build()
                .start();

        System.out.println("Servidor Xodus Environment corriendo en http://localhost:8084/xodus/env");
    }
}
