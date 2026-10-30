package com.example.devicetest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;

/** Menu, navigation, permissions and pass/fail results. The tests themselves are in Tests.java. */
public class MainActivity extends Activity {
    static final int BG = 0xFF0D1117, CARD = 0xFF1B222C, FG = 0xFFFFFFFF, MUTED = 0xFF9AA4B2;
    static final int GOOD = 0xFF22C55E, BAD = 0xFFEF4444;

    static final String[][] TESTS = {
            {"info", "Device info"},
            {"feat", "Hardware features"},
            {"display", "Display colours"},
            {"touch", "Touch screen"},
            {"camera", "Cameras and flash"},
            {"mic", "Microphone"},
            {"speaker", "Speaker"},
            {"vibe", "Vibration"},
            {"sens", "Sensors"},
            {"gps", "Location / GPS"},
            {"sim", "SIM, eSIM, VoLTE"},
            {"net", "Wi-Fi, Bluetooth, NFC"},
            {"batt", "Battery and charging"},
            {"keys", "Buttons and headset"},
    };

    final LinkedHashMap<String, Integer> results = new LinkedHashMap<String, Integer>();
    final Handler ui = new Handler(Looper.getMainLooper());

    Runnable cleanup;      // runs when a test screen is left
    Runnable pending;      // runs once a permission is granted
    boolean inTest;
    boolean keyTrap;       // the Buttons test wants to see every key
    TextView keyView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showMenu();
    }

    // ---------------------------------------------------------------- small helpers

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    TextView tv(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    void fullscreen(boolean on) {
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(0x1006); // immersive + hide bars
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    // ---------------------------------------------------------------- navigation

    void showMenu() {
        leave();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(16), dp(16), dp(24));
        col.addView(tv("Device Test", 26, FG));
        col.addView(tv("Tap a test, then mark Pass or Fail.", 14, MUTED));

        for (final String[] t : TESTS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(CARD);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.addView(tv(t[1], 17, FG), new LinearLayout.LayoutParams(0, -2, 1));
            Integer r = results.get(t[0]);
            String label = r == null ? "-" : (r.intValue() == 1 ? "PASS" : "FAIL");
            int color = r == null ? MUTED : (r.intValue() == 1 ? GOOD : BAD);
            row.addView(tv(label, 14, color));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tests.run(MainActivity.this, t[0]);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(8);
            col.addView(row, lp);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(20);
        col.addView(btn("Share report", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, report());
                startActivity(Intent.createChooser(i, "Share report"));
            }
        }), lp);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.addView(col);
        setContentView(sv);
    }

    String report() {
        StringBuilder sb = new StringBuilder(Tests.info(this)).append("\n\nResults\n");
        for (String[] t : TESTS) {
            Integer r = results.get(t[0]);
            sb.append(t[1]).append(": ")
                    .append(r == null ? "not tested" : (r.intValue() == 1 ? "PASS" : "FAIL"))
                    .append('\n');
        }
        return sb.toString();
    }

    /** A test screen: back button and title on top, your content, and Pass / Fail at the bottom. */
    void open(String title, final String key, View content, Runnable onLeave) {
        leave();
        cleanup = onLeave;
        inTest = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.addView(btn("<", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        }));
        TextView t = tv(title, 18, FG);
        t.setPadding(dp(12), 0, 0, 0);
        bar.addView(t);
        root.addView(bar);

        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        if (key != null) {
            LinearLayout foot = new LinearLayout(this);
            foot.setPadding(dp(8), dp(8), dp(8), dp(8));
            foot.addView(btn("PASS", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    results.put(key, 1);
                    showMenu();
                }
            }), new LinearLayout.LayoutParams(0, -2, 1));
            foot.addView(btn("FAIL", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    results.put(key, 2);
                    showMenu();
                }
            }), new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(foot);
        }
        setContentView(root);
    }

    /** A bare full-screen view with no bars (colour test). */
    void openRaw(View v, Runnable onLeave) {
        leave();
        cleanup = onLeave;
        inTest = true;
        setContentView(v);
    }

    void verdict(final String key) {
        fullscreen(false);
        new AlertDialog.Builder(this)
                .setMessage("Did it look right?")
                .setCancelable(false)
                .setPositiveButton("Pass", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        results.put(key, 1);
                        showMenu();
                    }
                })
                .setNegativeButton("Fail", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        results.put(key, 2);
                        showMenu();
                    }
                })
                .show();
    }

    void leave() {
        Runnable c = cleanup;
        cleanup = null;
        if (c != null) {
            try {
                c.run();
            } catch (Exception ignored) {
            }
        }
        inTest = false;
        keyTrap = false;
        keyView = null;
        fullscreen(false);
    }

    @Override
    public void onBackPressed() {
        if (inTest) showMenu();
        else super.onBackPressed();
    }

    // ---------------------------------------------------------------- permissions

    /** Runs r when the permissions are granted. If must is false it runs even when they are refused. */
    void withPerm(String[] perms, boolean must, Runnable r) {
        if (Build.VERSION.SDK_INT < 23) {
            r.run();
            return;
        }
        boolean ok = true;
        for (String p : perms) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) ok = false;
        }
        if (ok) {
            r.run();
            return;
        }
        pending = r;
        pendingMust = must;
        requestPermissions(perms, 1);
    }

    private boolean pendingMust;

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        Runnable r = pending;
        pending = null;
        if (r == null) return;
        boolean ok = res.length > 0;
        for (int g : res) {
            if (g != PackageManager.PERMISSION_GRANTED) ok = false;
        }
        if (ok || !pendingMust) r.run();
        else toast("Permission refused");
    }

    // ---------------------------------------------------------------- keys

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (keyTrap && code != KeyEvent.KEYCODE_BACK) {
            if (keyView != null) {
                keyView.setText("Last key: " + KeyEvent.keyCodeToString(code) + " (" + code + ")");
            }
            return true;
        }
        return super.onKeyDown(code, e);
    }
}
