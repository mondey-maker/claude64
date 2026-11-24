package com.claude64.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Saved servers for the "Join server" screen. */
final class ServerStore {
    static final class Server {
        final String name, host;
        final int port;
        Server(String name, String host, int port) { this.name = name; this.host = host; this.port = port; }
        @Override public String toString() { return name + "   —   " + host + ":" + port; }
    }

    private final SharedPreferences prefs;

    ServerStore(Context c) { prefs = c.getSharedPreferences("claude64", 0); }

    List<Server> all() {
        List<Server> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString("servers", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Server(o.getString("name"), o.getString("host"), o.getInt("port")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    void add(Server s) {
        List<Server> l = all();
        l.add(s);
        save(l);
    }

    void remove(int index) {
        List<Server> l = all();
        if (index >= 0 && index < l.size()) { l.remove(index); save(l); }
    }

    private void save(List<Server> l) {
        try {
            JSONArray a = new JSONArray();
            for (Server s : l) {
                JSONObject o = new JSONObject();
                o.put("name", s.name); o.put("host", s.host); o.put("port", s.port);
                a.put(o);
            }
            prefs.edit().putString("servers", a.toString()).apply();
        } catch (Exception ignored) { }
    }
}
