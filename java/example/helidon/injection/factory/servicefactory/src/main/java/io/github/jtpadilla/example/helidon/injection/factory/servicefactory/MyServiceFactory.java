package io.github.jtpadilla.example.helidon.injection.factory.servicefactory;

import io.helidon.service.registry.Qualifier;
import io.helidon.service.registry.Service;

import java.util.List;

@Service.Singleton
public class MyServiceFactory implements Service.ServicesFactory<MyService> {

    @Override
    public List<Service.QualifiedInstance<MyService>> services() {
        var named = Service.QualifiedInstance.create(new MyService(), Qualifier.createNamed("name"));
        var named2 = Service.QualifiedInstance.create(new MyService(), Qualifier.createNamed("name2"));
        return List.of(named, named2);
    }

}
