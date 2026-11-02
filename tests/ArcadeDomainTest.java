package tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ArcadeDomainTest {

    @Test
    @DisplayName("Execute full verified arcade domain test suite and real-frame benchmark")
    void testArcadeTestSuite() {
        ArcadeTestSuite.main(new String[0]);
    }
}
