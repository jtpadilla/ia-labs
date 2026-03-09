package io.github.jtpadilla.study.xodus.entity;

import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import jetbrains.exodus.entitystore.PersistentEntityStores;

import java.nio.file.Files;
import java.nio.file.Path;

public class EntityMain {

    public static void main(String[] args) throws Exception {
        LogConfig.configureRuntime();

        Path dbPath = Files.createTempDirectory("xodus-entity-");
        System.out.println("Base de datos Xodus Entity en: " + dbPath);

        var entityStore = PersistentEntityStores.newInstance(dbPath.toFile());

        WebServer server = WebServer.builder()
                .port(8084)
                .routing(routing -> routing
                        .get("/xodus/entity", (req, res) -> {
                            String entityId = entityStore.computeInTransaction(txn -> {
                                var user = txn.newEntity("User");
                                user.setProperty("name", "Gemini");
                                user.setProperty("type", "AI Agent");
                                return user.getId().toString();
                            });
                            
                            String name = entityStore.computeInReadonlyTransaction(txn -> {
                                var users = txn.getAll("User");
                                return users.getFirst().getProperty("name").toString();
                            });
                            res.send("Entity Store Layer: Creado usuario con ID " + entityId + " y nombre " + name);
                        })
                )
                .build()
                .start();

        System.out.println("Servidor Xodus Entity corriendo en http://localhost:8084/xodus/entity");
    }
}
