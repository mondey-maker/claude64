package com.claude64.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Claude64 main menu: Play, Host, Join server, Mods, ROM. The game itself runs in GameActivity. */
public class LauncherActivity extends Activity {
    private static final int REQ_ROM = 1, REQ_MOD = 2;
    private static final int PURPLE = Color.parseColor("#8A22C4");
    private static final int PURPLE_DARK = Color.parseColor("#5E1590");
    private static final int INK = Color.parseColor("#2A1340");

    private SharedPreferences prefs;
    private ServerStore servers;
    private TextView romStatus;
    private EditText nameBox;
    private Button playBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("claude64", 0);
        servers = new ServerStore(this);
        buildUi();
        installAssetsIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRomStatus();
    }

    // ---------------------------------------------------------------- UI

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private Button menuButton(String text, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(true);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(PURPLE);
        g.setCornerRadius(dp(14));
        btn.setBackground(g);
        btn.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(300), dp(52));
        lp.setMargins(0, dp(6), 0, dp(6));
        btn.setLayoutParams(lp);
        return btn;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.claude64_logo);
        logo.setAdjustViewBounds(true);
        logo.setLayoutParams(new LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(logo);

        romStatus = new TextView(this);
        romStatus.setTextColor(INK);
        romStatus.setTextSize(14);
        romStatus.setGravity(Gravity.CENTER);
        romStatus.setPadding(0, dp(4), 0, dp(8));
        root.addView(romStatus);

        nameBox = new EditText(this);
        nameBox.setHint("Player name");
        nameBox.setSingleLine(true);
        nameBox.setText(prefs.getString("name", "Player"));
        nameBox.setTextColor(INK);
        nameBox.setGravity(Gravity.CENTER);
        nameBox.setLayoutParams(new LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(nameBox);

        playBtn = menuButton("Play", v -> startGame());
        root.addView(playBtn);
        root.addView(menuButton("Host server", v -> hostDialog()));
        root.addView(menuButton("Join server", v -> serversDialog()));
        root.addView(menuButton("Mods", v -> modsDialog()));
        root.addView(menuButton("Import ROM", v -> pick(REQ_ROM, false)));

        TextView foot = new TextView(this);
        foot.setText("Claude64 • Super Mario 64 co-op for Android • bring your own ROM");
        foot.setTextColor(PURPLE_DARK);
        foot.setTextSize(11);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(10), 0, 0);
        root.addView(foot);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }

    private void refreshRomStatus() {
        boolean ok = RomImporter.hasRom(this);
        romStatus.setText(ok ? "ROM ready ✓" : "No ROM yet — tap IMPORT ROM and pick your Super Mario 64 (US) file");
        romStatus.setTextColor(ok ? Color.parseColor("#1B7F3B") : Color.parseColor("#B3261E"));
        playBtn.setAlpha(ok ? 1f : 0.5f);
    }

    // ---------------------------------------------------------------- launching

    private String playerName() {
        String n = nameBox.getText().toString().trim();
        if (n.isEmpty()) n = "Player";
        prefs.edit().putString("name", n).apply();
        return n;
    }

    private boolean checkReady() {
        if (!RomImporter.hasRom(this)) {
            Toast.makeText(this, "Import your Super Mario 64 (US) ROM first.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void launch(List<String> extra) {
        if (!checkReady()) return;
        List<String> args = new ArrayList<>();
        args.add("--playername");
        args.add(playerName());
        args.add("--skip-update-check");
        args.addAll(extra);
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_ARGS, args.toArray(new String[0]));
        startActivity(i);
    }

    private void startGame() { launch(new ArrayList<String>()); }

    private void hostDialog() {
        final EditText port = numberBox("7777");
        new AlertDialog.Builder(this)
                .setTitle("Host a server")
                .setMessage("Port (friends join with your IP and this port; forward it on your router for internet play, or use the same Wi-Fi):")
                .setView(pad(port))
                .setPositiveButton("Host", (d, w) -> {
                    List<String> a = new ArrayList<>();
                    a.add("--server");
                    a.add(port.getText().toString().trim());
                    launch(a);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void serversDialog() {
        final List<ServerStore.Server> list = servers.all();
        ArrayAdapter<ServerStore.Server> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle("Join a server");
        if (list.isEmpty()) b.setMessage("No saved servers yet. Add one with its IP / hostname and port.");
        else b.setAdapter(ad, (d, which) -> {
            ServerStore.Server s = list.get(which);
            List<String> a = new ArrayList<>();
            a.add("--client"); a.add(s.host); a.add(String.valueOf(s.port));
            launch(a);
        });
        b.setPositiveButton("Add server", (d, w) -> addServerDialog());
        if (!list.isEmpty()) b.setNeutralButton("Remove…", (d, w) -> removeServerDialog());
        b.setNegativeButton("Close", null);
        b.show();
    }

    private void addServerDialog() {
        final EditText name = textBox("Name (optional)");
        final EditText host = textBox("IP or hostname");
        final EditText port = numberBox("7777");
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(name); l.addView(host); l.addView(port);
        new AlertDialog.Builder(this)
                .setTitle("Add server")
                .setView(pad(l))
                .setPositiveButton("Save", (d, w) -> {
                    String h = host.getText().toString().trim();
                    int p;
                    try { p = Integer.parseInt(port.getText().toString().trim()); } catch (Exception e) { p = 7777; }
                    if (h.isEmpty() || p < 1 || p > 65535) {
                        Toast.makeText(this, "Enter a valid host and port.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String n = name.getText().toString().trim();
                    servers.add(new ServerStore.Server(n.isEmpty() ? h : n, h, p));
                    serversDialog();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeServerDialog() {
        final List<ServerStore.Server> list = servers.all();
        String[] items = new String[list.size()];
        for (int i = 0; i < items.length; i++) items[i] = list.get(i).toString();
        new AlertDialog.Builder(this).setTitle("Remove which server?")
                .setItems(items, (d, which) -> { servers.remove(which); serversDialog(); })
                .setNegativeButton("Back", (d, w) -> serversDialog())
                .show();
    }

    // ---------------------------------------------------------------- mods

    private void modsDialog() {
        final List<String> mods = ModManager.list(this);
        AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle("Mods");
        if (mods.isEmpty()) {
            b.setMessage("No extra mods installed.\n\nAdd a .zip, .lua or .mod file. Then turn mods on in the game menu: Host → Mods.");
        } else {
            b.setItems(mods.toArray(new String[0]), (d, which) ->
                    new AlertDialog.Builder(this).setTitle(mods.get(which))
                            .setMessage("Remove this mod?")
                            .setPositiveButton("Remove", (d2, w2) -> { ModManager.remove(this, mods.get(which)); modsDialog(); })
                            .setNegativeButton("Keep", (d2, w2) -> modsDialog())
                            .show());
        }
        b.setPositiveButton("Add mod…", (d, w) -> pick(REQ_MOD, true));
        b.setNegativeButton("Close", null);
        b.show();
    }

    private void pick(int req, boolean multiple) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if (multiple) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, req);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        final List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) {
            uris.add(data.getData());
        }
        final ProgressDialog pd = ProgressDialog.show(this, null, req == REQ_ROM ? "Checking ROM…" : "Adding mods…", true, false);
        new Thread(() -> {
            final StringBuilder msg = new StringBuilder();
            for (Uri u : uris) {
                String err = req == REQ_ROM ? RomImporter.importRom(this, u) : ModManager.add(this, u);
                if (err != null) msg.append(err).append('\n');
            }
            runOnUiThread(() -> {
                pd.dismiss();
                String m = msg.length() > 0 ? msg.toString().trim()
                        : (req == REQ_ROM ? "ROM imported." : "Mods added.");
                Toast.makeText(this, m, Toast.LENGTH_LONG).show();
                refreshRomStatus();
                if (req == REQ_MOD) modsDialog();
            });
        }).start();
    }

    // ---------------------------------------------------------------- helpers

    private void installAssetsIfNeeded() {
        if (!AssetInstaller.needsInstall(this)) return;
        final ProgressDialog pd = ProgressDialog.show(this, null, "Preparing game files…", true, false);
        new Thread(() -> {
            String err = null;
            try { AssetInstaller.install(this); } catch (Exception e) { err = e.getMessage(); }
            final String e2 = err;
            runOnUiThread(() -> {
                pd.dismiss();
                if (e2 != null) Toast.makeText(this, "Could not unpack game files: " + e2, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private EditText textBox(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        return e;
    }

    private EditText numberBox(String def) {
        EditText e = textBox("Port");
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(def);
        return e;
    }

    private View pad(View v) {
        LinearLayout l = new LinearLayout(this);
        l.setPadding(dp(20), dp(8), dp(20), 0);
        l.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }
}
