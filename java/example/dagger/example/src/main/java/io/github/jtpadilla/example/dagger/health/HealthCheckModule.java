package io.github.jtpadilla.example.dagger.health;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.StringKey;
import io.github.jtpadilla.example.dagger.health.checks.CpuCheck;
import io.github.jtpadilla.example.dagger.health.checks.DiskCheck;
import io.github.jtpadilla.example.dagger.health.checks.HealthCheck;
import io.github.jtpadilla.example.dagger.health.checks.MemoryCheck;
import io.github.jtpadilla.example.dagger.health.qualifiers.FastCheck;

import java.util.Set;

/**
 * Los Módulos son clases que enseñan a Dagger cómo proporcionar instancias de objetos
 * que no tienen @Inject en sus constructores (como interfaces o clases de librerías externas).
 */
@Module
public abstract class HealthCheckModule {

    /**
     * @Binds se usa para asignar una Interfaz a una Implementación concreta.
     * Es más eficiente que @Provides porque no genera un método en el código fuente final.
     * 
     * @IntoSet permite el patrón de "Multibinding" para crear un Set de chequeos.
     */
    @Binds
    @IntoSet
    abstract HealthCheck bindCpuCheck(CpuCheck cpuCheck);

    @Binds
    @IntoSet
    abstract HealthCheck bindMemoryCheck(MemoryCheck memoryCheck);

    @Binds
    @IntoSet
    abstract HealthCheck bindDiskCheck(DiskCheck diskCheck);

    /**
     * @IntoMap permite el patrón de "Multibinding" para crear un Map.
     * Es útil para buscar componentes por nombre o tipo.
     */
    @Binds
    @IntoMap
    @StringKey("cpu_status")
    abstract HealthCheck bindCpuCheckMap(CpuCheck cpuCheck);

    @Binds
    @IntoMap
    @StringKey("mem_status")
    abstract HealthCheck bindMemoryCheckMap(MemoryCheck memoryCheck);

    /**
     * @Provides se usa cuando se requiere lógica de creación más compleja o estática.
     * Aquí estamos usando un Calificador (@FastCheck) para diferenciar este Set
     * del Set general de todos los chequeos.
     */
    @Provides
    @FastCheck
    static Set<HealthCheck> provideFastChecks(CpuCheck cpu, MemoryCheck mem) {
        return Set.of(cpu, mem);
    }

}
