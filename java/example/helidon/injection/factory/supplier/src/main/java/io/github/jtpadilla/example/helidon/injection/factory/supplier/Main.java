package io.github.jtpadilla.example.helidon.injection.factory.supplier;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

@Service.Singleton
public class Main {

    public static void main(String[] args) {
        Main main = new Main(Services.get(MyService.class));
        System.out.println(main);
    }

    final private MyService myService;

    @Service.Inject
    public Main(MyService myService) {
        this.myService = myService;
    }

    @Override
    public String toString() {
        return "Main{" +
                "myService=" + myService.getText() +
                '}';
    }

}