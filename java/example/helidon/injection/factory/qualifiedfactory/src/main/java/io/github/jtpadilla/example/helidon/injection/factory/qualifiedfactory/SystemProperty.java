package io.github.jtpadilla.example.helidon.injection.factory.qualifiedfactory;

import io.helidon.service.registry.Service;

/* Se crea un cualificador que marca lo que sera una propiedad que se obtiene desde
el sistema.
 */
@Service.Qualifier
@interface SystemProperty {
    String value();
}
