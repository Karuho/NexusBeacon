package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyBeaconListenerMessages {
    private final String placed;
    private final String removed;
    private final String noPlacePermission;
    private final String notOwner;
    private final String inventoryFull;
    private final String transactionFailed;
    private final String duplicate;
    private final String invalidItem;

    public LegacyBeaconListenerMessages(String placed, String removed, String noPlacePermission,
            String notOwner, String inventoryFull, String transactionFailed, String duplicate,
            String invalidItem) {
        this.placed = required(placed);
        this.removed = required(removed);
        this.noPlacePermission = required(noPlacePermission);
        this.notOwner = required(notOwner);
        this.inventoryFull = required(inventoryFull);
        this.transactionFailed = required(transactionFailed);
        this.duplicate = required(duplicate);
        this.invalidItem = required(invalidItem);
    }

    public String getPlaced() { return placed; }
    public String getRemoved() { return removed; }
    public String getNoPlacePermission() { return noPlacePermission; }
    public String getNotOwner() { return notOwner; }
    public String getInventoryFull() { return inventoryFull; }
    public String getTransactionFailed() { return transactionFailed; }
    public String getDuplicate() { return duplicate; }
    public String getInvalidItem() { return invalidItem; }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("message is required");
        return value;
    }
}
