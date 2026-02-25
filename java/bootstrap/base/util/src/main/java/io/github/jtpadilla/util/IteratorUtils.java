package io.github.jtpadilla.util;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorUtils {

    public static <T> List<T> iteratorToList(Iterator<T> iterator) {

        // Convertir el Iterator a un Stream
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        Stream<T> stream = StreamSupport.stream(spliterator, false);

        // Colectar los elementos del Stream en una Lista
        return stream.collect(Collectors.toList());

    }

}
