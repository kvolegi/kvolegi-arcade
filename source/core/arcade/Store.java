package arcade;

/** Tiny key/value persistence implemented per platform. */
public interface Store {
    String get(String key, String def);
    void put(String key, String value);
    /** Removes every stored value (used by "reset progress"). */
    void clearAll();

    default int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); } catch (Exception e) { return def; }
    }
    default void putInt(String key, int v) { put(key, String.valueOf(v)); }
}
