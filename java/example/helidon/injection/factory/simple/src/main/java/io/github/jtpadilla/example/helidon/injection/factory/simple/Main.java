package io.github.jtpadilla.example.helidon.injection.factory.simple;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

@Service.Singleton
public class Main {

    public static void main(String[] args) {
        System.out.println(Services.get(Main.class));
    }

    final private MyContract myContract;

    @Service.Inject
    public Main(MyContract myContract) {
        this.myContract = myContract;
    }

    @Override
    public String toString() {
        return "Main{" +
                "myContract=" + myContract.getText() +
                '}';
    }

}