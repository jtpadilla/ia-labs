package io.github.jtpadilla.example.langchain4j.toolspecification.tool;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        GetCurrentTimeToolTest.class,
        FilterLocationsToolTest.class
})
public class ToolAllTests {
}
