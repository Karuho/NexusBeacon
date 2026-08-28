package cl.dynasty.nexusbeacon.platform.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class ClassicBeamRuntimeTest {
    private static final ClassicBeaconLocation LOCATION = new ClassicBeaconLocation("world", 4, 70, 8);

    @Test void authoritativeCustomBaseRendersConfiguredStyleIndependentOfRange() {
        Fixture f = new Fixture(record("aqua", false));
        f.world.customBase = true;
        f.runtime.start(); f.scheduler.tick();
        assertEquals(1, f.emitter.emissions);
        assertEquals("aqua", f.emitter.style.getId());
        assertFalse(f.record.isRangeParticles());
    }

    @Test void staleOrdinaryMissingAndUnavailableBeaconsNeverRender() {
        Fixture stale = new Fixture(record("aqua", true)); stale.records.authoritative = false; stale.runtime.start(); stale.scheduler.tick();
        Fixture ordinary = new Fixture(null); ordinary.runtime.start(); ordinary.scheduler.tick();
        Fixture missing = new Fixture(record("aqua", true)); missing.world.beacon = false; missing.runtime.start(); missing.scheduler.tick();
        Fixture unavailable = new Fixture(record("aqua", true)); unavailable.world.available = false; unavailable.runtime.start(); unavailable.scheduler.tick();
        assertEquals(0, stale.emitter.emissions + ordinary.emitter.emissions + missing.emitter.emissions + unavailable.emitter.emissions);
    }

    @Test void vanillaOnlyBaseIsSuppressedAndCustomBaseRenders() {
        Fixture f = new Fixture(record("aqua", true));
        f.runtime.start(); f.scheduler.tick(); assertEquals(0, f.emitter.emissions);
        f.world.customBase = true; f.scheduler.tick(); assertEquals(1, f.emitter.emissions);
    }

    @Test void removalStopsAnInProgressEmission() {
        Fixture f = new Fixture(record("aqua", true)); f.world.customBase = true;
        f.emitter.revokeDuringEmission = f.records;
        f.runtime.start(); f.scheduler.tick();
        assertEquals(1, f.emitter.emissions);
        assertFalse(f.emitter.lastAuthority.getAsBoolean());
        f.scheduler.tick(); assertEquals(1, f.emitter.emissions);
    }

    @Test void startAndRestartAreIdempotentAndDisableCancels() {
        Fixture f = new Fixture(record("aqua", true));
        f.runtime.start(); f.runtime.start();
        assertEquals(1, f.scheduler.schedules); assertEquals(1, f.runtime.getTaskCount());
        f.runtime.close(); assertEquals(1, f.scheduler.cancels); assertEquals(0, f.runtime.getTaskCount());
        f.runtime.start(); assertEquals(2, f.scheduler.schedules);
        f.runtime.close(); assertEquals(2, f.scheduler.cancels);
    }

    @Test void unsupportedStyleIsFilteredAndDustFallbackIsDeterministic() {
        YamlConfiguration config = config();
        config.set("beam-styles.broken.particle", "NOT_A_PARTICLE");
        ClassicBeamStyleCatalog catalog = new ClassicBeamStyleCatalog(config, new ClassicParticleResolver());
        assertNull(catalog.resolve("broken", "PLAYER"));
        ClassicBeamStyle aqua = catalog.resolve("aqua", "PLAYER");
        assertNotNull(aqua); assertEquals("REDSTONE", aqua.getParticle().name()); assertTrue(aqua.isDegraded());
        assertEquals(1, catalog.size());
    }

    private static ClassicBeaconRecord record(String style, boolean range) {
        return new ClassicBeaconRecord(LOCATION, UUID.randomUUID(), UUID.randomUUID(), 48, 1,
                Collections.<String,Integer>emptyMap(), Collections.<String>emptySet(), Collections.<UUID>emptySet(),
                true, style, range, "FLAME");
    }
    private static YamlConfiguration config() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("beam-styles.aqua.particle", "DUST"); c.set("beam-styles.aqua.color", "AQUA");
        c.set("visual-beam.global-style.particle", "END_ROD");
        return c;
    }

    private static final class Fixture {
        final ClassicBeaconRecord record; final FakeRecords records; final FakeWorld world = new FakeWorld();
        final FakeEmitter emitter = new FakeEmitter(); final FakeScheduler scheduler = new FakeScheduler(); final ClassicBeamRuntime runtime;
        Fixture(ClassicBeaconRecord record) {
            this.record = record; records = new FakeRecords(record);
            Set<Material> power = new HashSet<Material>(); power.add(Material.IRON_BLOCK); power.add(Material.REDSTONE_BLOCK);
            runtime = new ClassicBeamRuntime(records, world, emitter, scheduler,
                    new ClassicBeamRenderPolicy(true, "AUTO", 4, power),
                    new ClassicBeamStyleCatalog(config(), new ClassicParticleResolver()), "PLAYER_OR_GLOBAL", 8, .5D, 1, 4L);
        }
    }
    private static final class FakeRecords implements ClassicBeamRuntime.Records {
        ClassicBeaconRecord record; boolean authoritative = true;
        FakeRecords(ClassicBeaconRecord record){this.record=record;}
        public Collection<ClassicBeaconRecord> snapshot(){return record == null ? Collections.<ClassicBeaconRecord>emptyList() : Collections.singleton(record);}
        public boolean authoritative(ClassicBeaconRecord candidate){return authoritative && candidate == record;}
    }
    private static final class FakeWorld implements ClassicBeamRuntime.WorldView {
        boolean available=true, loaded=true, beacon=true, customBase;
        public boolean available(ClassicBeaconRecord r){return available;}
        public boolean chunkLoaded(ClassicBeaconRecord r){return loaded;}
        public boolean beaconPresent(ClassicBeaconRecord r){return beacon;}
        public Material materialAt(ClassicBeaconRecord r,int x,int y,int z){return customBase ? Material.REDSTONE_BLOCK : Material.IRON_BLOCK;}
    }
    private static final class FakeEmitter implements ClassicBeamRuntime.Emitter {
        int emissions; ClassicBeamStyle style; BooleanSupplier lastAuthority; FakeRecords revokeDuringEmission;
        public void emit(ClassicBeaconRecord r,ClassicBeamStyle s,int h,double step,int count,BooleanSupplier authority){emissions++;style=s;lastAuthority=authority;if(revokeDuringEmission!=null)revokeDuringEmission.authoritative=false;}
    }
    private static final class FakeScheduler implements SchedulerService {
        int schedules,cancels; Runnable timer;
        public ScheduledTaskHandle runSyncTimer(Runnable r,long delay,long interval){schedules++;timer=r;return ()->cancels++;}
        void tick(){timer.run();}
        public void runSync(Runnable r){r.run();} public void runSync(Location l,Runnable r){r.run();} public void runSync(Entity e,Runnable r){r.run();}
        public ScheduledTaskHandle runSyncLater(Runnable r,long d){return ()->{};} public void runAsync(Runnable r){r.run();}
        public ScheduledTaskHandle runAsyncLater(Runnable r,long d){return ()->{};} public ScheduledTaskHandle runAsyncTimer(Runnable r,long d,long i){return ()->{};}
    }
}
