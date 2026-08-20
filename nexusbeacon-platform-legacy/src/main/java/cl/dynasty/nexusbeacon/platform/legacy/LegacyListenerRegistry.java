package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** Owns one explicit listener set and its register/unregister lifecycle. */
public final class LegacyListenerRegistry {
    interface Registrar {
        void register(Listener listener);
        void unregister(Listener listener);
    }

    private final List<Listener> listeners;
    private final Registrar registrar;
    private boolean registered;

    public LegacyListenerRegistry(final Plugin plugin, List<? extends Listener> listeners) {
        this(new Registrar() {
            @Override public void register(Listener listener) {
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            }

            @Override public void unregister(Listener listener) {
                HandlerList.unregisterAll(listener);
            }
        }, listeners);
        if (plugin == null) throw new NullPointerException("plugin");
    }

    LegacyListenerRegistry(Registrar registrar, List<? extends Listener> listeners) {
        if (registrar == null) throw new NullPointerException("registrar");
        if (listeners == null || listeners.isEmpty()) {
            throw new IllegalArgumentException("listeners must not be empty");
        }
        List<Listener> copy = new ArrayList<Listener>(listeners.size());
        for (Listener listener : listeners) {
            if (listener == null) throw new NullPointerException("listener");
            if (copy.contains(listener)) throw new IllegalArgumentException("duplicate listener instance");
            copy.add(listener);
        }
        this.registrar = registrar;
        this.listeners = Collections.unmodifiableList(copy);
    }

    public boolean register() {
        if (registered) return false;
        int completed = 0;
        try {
            for (Listener listener : listeners) {
                registrar.register(listener);
                completed++;
            }
        } catch (RuntimeException failure) {
            for (int index = completed - 1; index >= 0; index--) {
                registrar.unregister(listeners.get(index));
            }
            throw failure;
        }
        registered = true;
        return true;
    }

    public boolean unregister() {
        if (!registered) return false;
        for (int index = listeners.size() - 1; index >= 0; index--) {
            registrar.unregister(listeners.get(index));
        }
        registered = false;
        return true;
    }

    public boolean isRegistered() { return registered; }
    public int size() { return listeners.size(); }
    public List<Listener> getListeners() { return listeners; }
}
