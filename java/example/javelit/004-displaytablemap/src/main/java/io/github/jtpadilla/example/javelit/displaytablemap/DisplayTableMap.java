package io.github.jtpadilla.example.javelit.displaytablemap;

import io.javelit.core.Jt;
import io.javelit.core.Server;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.List;

class DisplayTableMap {

    static void main(String[] args) {
        Server.builder(DisplayTableMap::javelit, 8080)
                .build()
                .start();

    }

    final static String PRODUCT = "Product";
    final static String PRICE = "Price";
    final static String STOCK = "Stock";

    final static List<String> PRODUCT_ROWS = List.of("Apple", "Banana", "Orange");
    final static List<Double> PRICE_ROWS = List.of(1.2, 0.5, 0.8);
    final static List<Integer> STOCK_ROWS = List.of(100, 150, 75);

    static private Table createTable() {

        final StringColumn productColumn = StringColumn.create(PRODUCT);
        PRODUCT_ROWS.forEach(productColumn::append);

        final DoubleColumn priceColumn = DoubleColumn.create(PRICE);
        PRICE_ROWS.forEach(priceColumn::append);

        final IntColumn stockColumn = IntColumn.create(STOCK);
        STOCK_ROWS.forEach(stockColumn::append);

        return Table.create("Store").addColumns(
                productColumn,
                priceColumn,
                stockColumn
        );

    }

    static private void javelit() {
        Jt.table(createTable()).use();
    }

}