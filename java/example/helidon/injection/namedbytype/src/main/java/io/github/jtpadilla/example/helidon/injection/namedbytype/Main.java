package io.github.jtpadilla.example.helidon.injection.namedbytype;

import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

@Service.Singleton
public class Main {

    public static void main(String[] args) {
        Main main = Services.get(Main.class);
        System.out.println(main);
    }

    final public BlueCircle blueCircle;
    final public GreenCircle greenCircle;

    @Service.Inject
    public Main(BlueCircle blueCircle, GreenCircle greenCircle) {
        this.blueCircle = blueCircle;
        this.greenCircle = greenCircle;
    }

    @Override
    public String toString() {
        return "Main{" +
                "blueCircle=" + blueCircle +
                ", greenCircle=" + greenCircle +
                '}';
    }

}