package com.mamiyaotaru.voxelmap.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuiAddWaypointTest {
    @Test
    void parsesWhitespaceSeparatedCoordinateTriples() {
        assertEquals(List.of("1", "2", "3"), GuiAddWaypoint.parseCoordinateTriple("1 2 3"));
        assertEquals(List.of("-1", "20", "300"), GuiAddWaypoint.parseCoordinateTriple("  -1   20  300  "));
        assertEquals(List.of("+01", "-002", "3"), GuiAddWaypoint.parseCoordinateTriple("+01\t-002\n3"));
    }

    @Test
    void parsesCommaSeparatedCoordinateTriples() {
        assertEquals(List.of("1", "2", "3"), GuiAddWaypoint.parseCoordinateTriple("1,2,3"));
        assertEquals(List.of("-1", "20", "300"), GuiAddWaypoint.parseCoordinateTriple("  -1, 20 , 300  "));
    }

    @Test
    void rejectsInvalidCoordinateTriples() {
        List<String> invalidInputs = List.of(
                "",
                "1",
                "1 2",
                "1 2 3 4",
                "1,,3",
                "1,2,",
                "1.0 2 3",
                "1, 2 3",
                "2147483648 2 3",
                "-2147483649,2,3"
        );

        for (String input : invalidInputs) {
            assertTrue(GuiAddWaypoint.parseCoordinateTriple(input).isEmpty(), input);
        }
        assertTrue(GuiAddWaypoint.parseCoordinateTriple(null).isEmpty());
    }
}
