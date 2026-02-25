package io.github.jtpadilla.unit.test;

import io.github.jtpadilla.unit.test.logger.*;
import io.github.jtpadilla.unit.test.parameter.ParameterTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ServiceSimpleTests.class,
        ServiceEfectiveTests.class,
        ServiceNonEffectiveTests.class,
        ServiceListTests.class,
        ServiceEnabledLevelTests.class,
        ParameterTest.class,
    })
public class UnitAllTests {
}
