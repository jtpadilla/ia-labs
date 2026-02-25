package io.github.jtpadilla.unit.logger.impl;

import io.github.jtpadilla.unit.logger.UnitLoggerLevel;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base de datos jerárquica que almacena registros en una estructura de árbol
 * usando colecciones concurrentes para garantizar thread-safety.
 */
public class DB {

    // Separador de segmentos en nombres jerárquicos
    private static final String SEPARATOR = ".";

    // Root logger, usado como punto de entrada para la jerarquía
    private final Entry rootLogger;

    // Mapa concurrente para almacenar todos los loggers
    private final ConcurrentMap<String, Entry> loggerMap;

    // Lock para operaciones que modifican la estructura del árbol
    private final ReadWriteLock treeLock = new ReentrantReadWriteLock();

    public DB() {
        this.loggerMap = new ConcurrentHashMap<>();
        this.rootLogger = new RootEntry();
        this.loggerMap.put("", rootLogger);
    }

    /**
     * Obtiene o crea un logger con el nombre especificado.
     */
    public Entry getLogger(String name) {

        if (name == null || name.isEmpty()) {
            return rootLogger;
        }

        // Verificar si el logger ya existe
        Entry logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        }

        // Si no existe, necesitamos crear el logger y sus padres si es necesario
        treeLock.writeLock().lock();
        try {
            // Verificar de nuevo en caso de que otro hilo lo haya creado
            logger = loggerMap.get(name);
            if (logger != null) {
                return logger;
            }

            // Encontrar el padre
            String parentName = getParentName(name);
            Entry parent = getLogger(parentName); // Recursivamente obtiene o crea el padre

            // Crear el logger
            logger = new Entry(name); // Nivel nulo significa heredado
            logger.parent = parent;
            loggerMap.put(name, logger);
            parent.addChild(logger);

            return logger;
        } finally {
            treeLock.writeLock().unlock();
        }
    }

    /**
     * Establece el nivel para un logger específico.
     */
    public void setLevel(String name, UnitLoggerLevel level) {
        Entry logger = getLogger(name);
        logger.setLevel(level);
    }

    /**
     * Obtiene el nivel efectivo para un nombre de logger, considerando la herencia.
     */
    public UnitLoggerLevel getEffectiveLevel(String name) {
        Entry logger = getLogger(name);
        return logger.getEffectiveLevel();
    }

    /**
     * Verifica si un nivel es loggeable para un nombre específico.
     */
    public boolean isLoggable(String name, UnitLoggerLevel level) {
        UnitLoggerLevel effectiveLevel = getEffectiveLevel(name);
        return effectiveLevel.isLoggable(level);
    }

    /**
     * Devuelve el nombre del padre para un nombre de logger.
     */
    private String getParentName(String name) {
        int lastDotIndex = name.lastIndexOf(SEPARATOR);
        if (lastDotIndex > 0) {
            return name.substring(0, lastDotIndex);
        } else {
            return ""; // Root logger
        }
    }

    /**
     * Devuelve una vista de todos los loggers existentes.
     */
    public Set<String> getLoggerNames() {
        return Collections.unmodifiableSet(loggerMap.keySet());
    }

    /**
     * Restablece toda la base de datos jerárquica a su estado inicial.
     */
    public void reset() {
        treeLock.writeLock().lock();
        try {
            loggerMap.clear();
            rootLogger.setLevel(UnitLoggerLevel.INFO);
            loggerMap.put("", rootLogger);
        } finally {
            treeLock.writeLock().unlock();
        }
    }

}
