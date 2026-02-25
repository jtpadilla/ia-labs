package io.github.jtpadilla.server.agentserver;

import com.google.inject.Inject;
import com.mycila.guice.ext.closeable.InjectorCloseListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentServer implements InjectorCloseListener {

    private final ScheduledExecutorService executorService;

    @Inject
    public AgentServer() {

        // Crea un ScheduledExecutorService con un único hilo
        this.executorService = Executors.newSingleThreadScheduledExecutor();

        // Programa la tarea periódica cada 10 segundos
        executorService.scheduleAtFixedRate(this::printHello, 0, 10, TimeUnit.SECONDS);
    }

    private void printHello() {
        System.out.println("Hello World!");
    }

    @Override
    public void onInjectorClosing() {

        // Detiene el ScheduledExecutorService al cerrar el inyector
        System.out.println("Deteniendo el sistema...");
        executorService.shutdown();
        try {
            // Espera hasta 5 segundos para completar tareas pendientes
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Forzar cierre si no termina dentro del tiempo
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt(); // Restaura el estado de interrupción del hilo
        }
        System.out.println("Sistema detenido.");
    }

}