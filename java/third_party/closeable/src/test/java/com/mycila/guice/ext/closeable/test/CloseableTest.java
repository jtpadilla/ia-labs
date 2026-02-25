package com.mycila.guice.ext.closeable.test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import com.mycila.guice.ext.closeable.CloseableInjector;
import com.mycila.guice.ext.closeable.CloseableModule;
import com.mycila.guice.ext.closeable.InjectorCloseListener;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Singleton;

public final class CloseableTest {

    @Test
    public void test() throws Exception {

        com.google.inject.Module module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(MustClose.class).in(Singleton.class);
            }
        };

        CloseableInjector injector = Guice.createInjector(Stage.PRODUCTION, new CloseableModule(), module)
                .getInstance(CloseableInjector.class);

        Assert.assertEquals(0, MustClose.hits);
        injector.close();
        Assert.assertEquals(1, MustClose.hits);

    }

    static class MustClose implements InjectorCloseListener {

        static int hits;

        @Override
        public void onInjectorClosing() {
            hits++;
        }

    }

}
