package arcade.android;

import android.content.Context;
import android.content.SharedPreferences;
import arcade.Store;

public class PrefsStore implements Store {
    private final SharedPreferences prefs;

    public PrefsStore(Context context) {
        this.prefs = context.getSharedPreferences("kvolegi_arcade_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public String get(String key, String def) {
        return prefs.getString(key, def);
    }

    @Override
    public void put(String key, String val) {
        prefs.edit().putString(key, val).apply();
    }

    @Override
    public int getInt(String key, int def) {
        return prefs.getInt(key, def);
    }

    @Override
    public void putInt(String key, int val) {
        prefs.edit().putInt(key, val).apply();
    }

    @Override
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
