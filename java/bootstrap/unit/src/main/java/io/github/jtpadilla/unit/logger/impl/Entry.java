package io.github.jtpadilla.unit.logger.impl;

import io.github.jtpadilla.unit.logger.UnitLoggerLevel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Representa un nodo en la jerarquía de loggers.
 * Thread-safe para operaciones de lectura/escritura.
 */
public class Entry {

    public Entry parent;
    private final String name;
    protected volatile UnitLoggerLevel level; // Volatile para garantizar visibilidad
    private final Set<Entry> children = Collections.synchronizedSet(new HashSet<>());

    public Entry(String name) {
        this.name = name;
        this.level = null;
    }

    public String getName() {
        return name;
    }

    public void setLevel(UnitLoggerLevel level) {
        this.level = level;
    }

    public Optional<UnitLoggerLevel> getLevel() {
        return Optional.ofNullable(level);
    }

    /**
     * Obtiene el nivel efectivo considerando la herencia.
     */
    public UnitLoggerLevel getEffectiveLevel() {
        Entry current = this;
        while (current != null) {
            Optional<UnitLoggerLevel> lvl = current.getLevel();
            if (lvl.isPresent()) {
                return lvl.get();
            }
            current = current.parent;
        }
        throw new IllegalStateException("getEffectiveLevel() ha escalado hasta ROOT sin encontrar un nivel efectivo.");
        // return LocalLoggerLevel.INFO; // Nivel por defecto si no hay configuración
    }

    /**
     * Determina si un nivel es loggeable para este logger.
     */
    public boolean isLoggable(UnitLoggerLevel messageLevel) {
        UnitLoggerLevel effectiveLevel = getEffectiveLevel();
        return effectiveLevel.isLoggable(messageLevel);
    }

    public void addChild(Entry child) {
        children.add(child);
    }

    /**
     * Obtiene una copia segura de los hijos de este logger.
     */
    public Set<Entry> getChildren() {
        return Collections.unmodifiableSet(new HashSet<>(children));
    }

}

