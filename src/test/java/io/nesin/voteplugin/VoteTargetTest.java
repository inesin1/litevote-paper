package io.nesin.voteplugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VoteTargetTest {

    @ParameterizedTest
    @CsvSource({
            "day, DAY",
            "night, NIGHT",
            "clear, CLEAR",
            "rain, RAIN",
            "storm, STORM"
    })
    void testValidTargets(String input, VoteTarget expected) {
        assertEquals(expected, VoteTarget.from(input));
    }

    @Test
    void testInvalidTargetReturnsNull() {
        assertNull(VoteTarget.from("invalid"));
        assertNull(VoteTarget.from(""));
        assertNull(VoteTarget.from(null));
    }
}
