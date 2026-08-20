package cl.dynasty.nexusbeacon.platform.modern;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;

class ModernPlatformFactoryTest {
    @Test
    void composesFoliaSchedulerAndPaperTeleporter() {
        PlatformServices services = ModernPlatformFactory.create(null, true, true);
        assertInstanceOf(ModernFoliaSchedulerService.class, services.getScheduler());
        assertInstanceOf(ModernPaperTeleporterService.class, services.getTeleporter());
    }

    @Test
    void composesBukkitSchedulerAndBukkitTeleporter() {
        PlatformServices services = ModernPlatformFactory.create(null, false, false);
        assertInstanceOf(ModernBukkitSchedulerService.class, services.getScheduler());
        assertInstanceOf(ModernBukkitTeleporterService.class, services.getTeleporter());
    }

    @Test
    void keepsSchedulerAndTeleporterCombinationsIndependent() {
        PlatformServices foliaFallback = ModernPlatformFactory.create(null, true, false);
        PlatformServices bukkitPaper = ModernPlatformFactory.create(null, false, true);
        assertInstanceOf(ModernFoliaSchedulerService.class, foliaFallback.getScheduler());
        assertInstanceOf(ModernBukkitTeleporterService.class, foliaFallback.getTeleporter());
        assertInstanceOf(ModernBukkitSchedulerService.class, bukkitPaper.getScheduler());
        assertInstanceOf(ModernPaperTeleporterService.class, bukkitPaper.getTeleporter());
    }

    @Test
    void platformServicesRetainsExactContractsAndRejectsNulls() {
        PlatformServices services = ModernPlatformFactory.create(null, false, false);
        SchedulerService scheduler = services.getScheduler();
        TeleporterService teleporter = services.getTeleporter();
        assertSame(scheduler, services.getScheduler());
        assertSame(teleporter, services.getTeleporter());
        assertThrows(NullPointerException.class, () -> new PlatformServices(null, teleporter));
        assertThrows(NullPointerException.class, () -> new PlatformServices(scheduler, null));
    }
}
