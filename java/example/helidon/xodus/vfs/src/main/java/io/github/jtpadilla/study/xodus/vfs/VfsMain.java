package io.github.jtpadilla.study.xodus.vfs;

import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.vfs.VfsConfig;
import jetbrains.exodus.vfs.VirtualFileSystem;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VfsMain {

    public static void main(String[] args) throws Exception {
        LogConfig.configureRuntime();

        Path dbPath = Files.createTempDirectory("xodus-vfs-");
        System.out.println("Base de datos Xodus VFS en: " + dbPath);

        var env = Environments.newInstance(dbPath.toFile());
        var vfs = new VirtualFileSystem(env, VfsConfig.DEFAULT);

        WebServer server = WebServer.builder()
                .port(8084)
                .routing(routing -> routing
                        .get("/xodus/vfs", (req, res) -> {
                            env.executeInTransaction(txn -> {
                                var file = vfs.createFile(txn, "readme.txt");
                                try (var os = vfs.writeFile(txn, file)) {
                                    os.write("Contenido del archivo virtual".getBytes(StandardCharsets.UTF_8));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            
                            String content = env.computeInReadonlyTransaction(txn -> {
                                var file = vfs.openFile(txn, "readme.txt", false);
                                try (InputStream is = vfs.readFile(txn, file)) {
                                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                } catch (Exception e) {
                                    return "Error leyendo VFS";
                                }
                            });
                            res.send("VFS Layer: " + content);
                        })
                )
                .build()
                .start();

        System.out.println("Servidor Xodus VFS corriendo en http://localhost:8084/xodus/vfs");
    }
}
