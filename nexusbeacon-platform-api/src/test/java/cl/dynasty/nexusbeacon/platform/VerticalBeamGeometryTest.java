package cl.dynasty.nexusbeacon.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.VerticalBeamGeometry;

class VerticalBeamGeometryTest {
    @Test void preservesCurrentDefaultPointDensity() {
        assertEquals(214, VerticalBeamGeometry.pointCount(96, 0.45D));
    }

    @Test void emitsStraightVerticalPointsWithoutMutatingBase() {
        Location base = new Location(null, 4.5D, 65.0D, -2.5D);
        List<Location> points = new ArrayList<Location>();

        VerticalBeamGeometry.forEachPoint(base, 1, 0.5D, points::add);

        assertEquals(3, points.size());
        assertEquals(4.5D, points.get(2).getX());
        assertEquals(66.0D, points.get(2).getY());
        assertEquals(-2.5D, points.get(2).getZ());
        assertEquals(65.0D, base.getY());
    }

    @Test void rejectsInvalidGeometry() {
        assertThrows(IllegalArgumentException.class, () -> VerticalBeamGeometry.pointCount(0, 0.45D));
        assertThrows(IllegalArgumentException.class, () -> VerticalBeamGeometry.pointCount(96, 0.0D));
    }
}
