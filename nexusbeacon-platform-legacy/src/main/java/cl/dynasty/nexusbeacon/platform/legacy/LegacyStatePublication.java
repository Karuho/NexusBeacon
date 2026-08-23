package cl.dynasty.nexusbeacon.platform.legacy;

/** Internal publication boundary; injectable from tests without production failure switches. */
interface LegacyStatePublication {
    LegacyStatePublication DIRECT = new LegacyStatePublication() {
        @Override public void beforePublish() { }
    };

    void beforePublish();
}
