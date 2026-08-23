package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;

class LegacyListenerRegistryTest {
    @Test
    void registersAndUnregistersEachListenerExactlyOnce() {
        CountingRegistrar registrar = new CountingRegistrar();
        LegacyListenerRegistry registry = new LegacyListenerRegistry(registrar,
                Arrays.asList(new TestListener(), new TestListener()));

        assertTrue(registry.register());
        assertFalse(registry.register());
        assertEquals(2, registrar.registrations);
        assertTrue(registry.isRegistered());

        assertTrue(registry.unregister());
        assertFalse(registry.unregister());
        assertEquals(2, registrar.unregistrations);
        assertFalse(registry.isRegistered());
    }

    @Test
    void rejectsDuplicateListenerInstances() {
        TestListener listener = new TestListener();
        assertThrows(IllegalArgumentException.class, () -> new LegacyListenerRegistry(
                new CountingRegistrar(), Arrays.asList(listener, listener)));
    }

    @Test
    void rollsBackARegistrationFailure() {
        CountingRegistrar registrar = new CountingRegistrar();
        registrar.failAtRegistration = 2;
        LegacyListenerRegistry registry = new LegacyListenerRegistry(registrar,
                Arrays.asList(new TestListener(), new TestListener()));

        assertThrows(IllegalStateException.class, registry::register);
        assertEquals(2, registrar.registrations);
        assertEquals(1, registrar.unregistrations);
        assertFalse(registry.isRegistered());
    }

    @Test
    void rejectsAnEmptyGraph() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyListenerRegistry(
                new CountingRegistrar(), Collections.<Listener>emptyList()));
    }

    private static final class TestListener implements Listener {}

    private static final class CountingRegistrar implements LegacyListenerRegistry.Registrar {
        private int registrations;
        private int unregistrations;
        private int failAtRegistration;

        @Override public void register(Listener listener) {
            registrations++;
            if (registrations == failAtRegistration) throw new IllegalStateException("registration failed");
        }

        @Override public void unregister(Listener listener) {
            unregistrations++;
        }
    }
}
