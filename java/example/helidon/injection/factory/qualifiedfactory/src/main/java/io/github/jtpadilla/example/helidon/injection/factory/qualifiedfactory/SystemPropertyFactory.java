package io.github.jtpadilla.example.helidon.injection.factory.qualifiedfactory;

import io.helidon.common.GenericType;
import io.helidon.service.registry.Lookup;
import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import java.util.Optional;

@Service.Singleton
class SystemPropertyFactory implements Service.QualifiedFactory<String, SystemProperty> {

    @Override
    public Optional<Service.QualifiedInstance<String>> first(
    Qualifier qualifier,
            Lookup lookup,
            GenericType<String> genericType) {
        return qualifier.stringValue()
                .map(System::getProperty)
                .map(propertyValue -> Service.QualifiedInstance.create(propertyValue, qualifier));
    }

}