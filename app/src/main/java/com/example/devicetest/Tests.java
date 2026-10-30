package com.example.devicetest;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.Vibrator;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** All the test screens. Each one reads real hardware state and shows it as plain text. */
final class Tests {
    private Tests() {
    }

    static void run(MainActivity a, String key) {
        if (key.equals("info")) a.open("Device info", null, text(a, info(a)), null);
        else if (key.equals("feat")) a.open("Hardware features", null, text(a, features(a)), null);
        else if (key.equals("display")) display(a);
        else if (key.equals("touch")) a.open("Touch screen", "touch", new TouchView(a), null);
        else if (key.equals("camera")) camera(a);
        else if (key.equals("mic")) mic(a);
        else if (key.equals("speaker")) speaker(a);
        else if (key.equals("vibe")) vibe(a);
        else if (key.equals("sens")) sensors(a);
        else if (key.equals("gps")) gps(a);
        else if (key.equals("sim")) sim(a);
        else if (key.equals("net")) net(a);
        else if (key.equals("batt")) batt(a);
        else if (key.equals("keys")) keys(a);
    }

    // ---------------------------------------------------------------- layout helpers

    static LinearLayout col(MainActivity a) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(16));
        return c;
    }

    static View wrap(MainActivity a, View v) {
        ScrollView sv = new ScrollView(a);
        sv.addView(v);
        return sv;
    }

    static View text(MainActivity a, String s) {
        LinearLayout c = col(a);
        c.addView(a.tv(s, 15, MainActivity.FG));
        return wrap(a, c);
    }

    static String name(String[] names, int i) {
        return i >= 0 && i < names.length ? names[i] : "Unknown (" + i + ")";
    }

    // ---------------------------------------------------------------- info and features

    static String info(MainActivity a) {
        DisplayMetrics m = a.getResources().getDisplayMetrics();
        StringBuilder s = new StringBuilder();
        s.append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        s.append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        if (Build.VERSION.SDK_INT >= 23) {
            s.append("Security patch: ").append(Build.VERSION.SECURITY_PATCH).append('\n');
        }
        s.append("Build: ").append(Build.DISPLAY).append('\n');
        s.append("Hardware: ").append(Build.HARDWARE).append('\n');
        s.append("CPU ABI: ");
        if (Build.VERSION.SDK_INT >= 21) {
            for (String abi : Build.SUPPORTED_ABIS) s.append(abi).append(' ');
        } else {
            s.append(Build.CPU_ABI).append(' ').append(Build.CPU_ABI2);
        }
        s.append("\nCPU cores: ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ((ActivityManager) a.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
            s.append("RAM: ").append(mi.totalMem >> 20).append(" MB total, ")
                    .append(mi.availMem >> 20).append(" MB free\n");
        }
        StatFs st = new StatFs(Environment.getDataDirectory().getPath());
        long bs = st.getBlockSize();
        s.append("Storage: ").append((bs * st.getAvailableBlocks()) >> 20).append(" MB free of ")
                .append((bs * st.getBlockCount()) >> 20).append(" MB\n");
        s.append("Screen: ").append(m.widthPixels).append('x').append(m.heightPixels).append(", ")
                .append(m.densityDpi).append(" dpi, ")
                .append(String.format(Locale.US, "%.0f Hz", a.getWindowManager().getDefaultDisplay().getRefreshRate()))
                .append('\n');
        s.append("Kernel: ").append(System.getProperty("os.version")).append('\n');
        s.append("Uptime: ").append(SystemClock.elapsedRealtime() / 60000).append(" min\n");
        return s.toString();
    }

    static final String[] FEATURES = {
            "camera", "camera.front", "camera.flash", "camera.autofocus", "microphone",
            "touchscreen.multitouch", "touchscreen.multitouch.distinct",
            "bluetooth", "bluetooth_le", "nfc", "nfc.hce", "wifi", "wifi.direct", "wifi.aware",
            "location.gps", "location.network",
            "telephony", "telephony.gsm", "telephony.cdma", "telephony.euicc", "telephony.ims",
            "sensor.accelerometer", "sensor.gyroscope", "sensor.compass", "sensor.light",
            "sensor.proximity", "sensor.barometer", "sensor.stepcounter", "sensor.heartrate",
            "fingerprint", "usb.host", "usb.accessory", "audio.low_latency", "audio.pro",
            "consumerir", "vulkan.level", "opengles.aep", "ethernet", "uwb",
    };

    static String features(MainActivity a) {
        PackageManager pm = a.getPackageManager();
        StringBuilder s = new StringBuilder("Hardware the phone says it has:\n\n");
        for (String f : FEATURES) {
            s.append(pm.hasSystemFeature("android.hardware." + f) ? "[yes]  " : "[ no ]  ").append(f).append('\n');
        }
        return s.toString();
    }

    // ---------------------------------------------------------------- display

    static void display(final MainActivity a) {
        final int[] cols = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000, 0xFF808080};
        final int[] i = {0};
        final View v = new View(a);
        v.setBackgroundColor(cols[0]);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                i[0]++;
                if (i[0] >= cols.length) a.verdict("display");
                else v.setBackgroundColor(cols[i[0]]);
            }
        });
        a.openRaw(v, null);
        a.fullscreen(true);
        a.toast("Tap to change colour. Look for dead pixels.");
    }

    // ---------------------------------------------------------------- camera

    static void stopCamera(Camera[] cam) {
        Camera c = cam[0];
        cam[0] = null;
        if (c != null) {
            try {
                c.stopPreview();
            } catch (Exception ignored) {
            }
            try {
                c.release();
            } catch (Exception ignored) {
            }
        }
    }

    static int cameraRotation(MainActivity a, Camera.CameraInfo ci) {
        int deg = a.getWindowManager().getDefaultDisplay().getRotation() * 90;
        if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (ci.orientation + deg) % 360) % 360;
        }
        return (ci.orientation - deg + 360) % 360;
    }

    static void camera(final MainActivity a) {
        a.withPerm(new String[]{Manifest.permission.CAMERA}, true, new Runnable() {
            @Override
            public void run() {
                final int n = Camera.getNumberOfCameras();
                final FrameLayout box = new FrameLayout(a);
                final SurfaceView sv = new SurfaceView(a);
                box.addView(sv, new FrameLayout.LayoutParams(-1, -1));

                final TextView info = a.tv(n == 0 ? "No cameras found" : "", 13, MainActivity.FG);
                info.setBackgroundColor(0x99000000);
                box.addView(info, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT));

                final Camera[] cam = new Camera[1];
                final int[] id = {0};
                final boolean[] torch = {false};

                final Runnable start = new Runnable() {
                    @Override
                    public void run() {
                        stopCamera(cam);
                        if (n == 0) return;
                        try {
                            Camera c = Camera.open(id[0]);
                            Camera.CameraInfo ci = new Camera.CameraInfo();
                            Camera.getCameraInfo(id[0], ci);
                            Camera.Parameters p = c.getParameters();
                            Camera.Size best = null;
                            for (Camera.Size s : p.getSupportedPreviewSizes()) {
                                if (s.width * s.height <= 1280 * 720
                                        && (best == null || s.width * s.height > best.width * best.height)) {
                                    best = s;
                                }
                            }
                            if (best != null) p.setPreviewSize(best.width, best.height);
                            List<String> fm = p.getSupportedFlashModes();
                            boolean flash = fm != null && fm.contains(Camera.Parameters.FLASH_MODE_TORCH);
                            if (flash && torch[0]) p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            c.setParameters(p);
                            c.setDisplayOrientation(cameraRotation(a, ci));
                            c.setPreviewDisplay(sv.getHolder());
                            c.startPreview();
                            cam[0] = c;
                            info.setText("Camera " + (id[0] + 1) + " of " + n + " ("
                                    + (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back") + ")  "
                                    + (best == null ? "" : best.width + "x" + best.height)
                                    + "  torch: " + (flash ? "yes" : "no"));
                        } catch (Exception e) {
                            info.setText("Camera error: " + e.getMessage());
                        }
                    }
                };

                sv.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder h) {
                        start.run();
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder h) {
                        stopCamera(cam);
                    }
                });

                LinearLayout bar = new LinearLayout(a);
                bar.addView(a.btn("Switch camera", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (n > 0) id[0] = (id[0] + 1) % n;
                        start.run();
                    }
                }));
                bar.addView(a.btn("Torch", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        torch[0] = !torch[0];
                        start.run();
                    }
                }));
                box.addView(bar, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

                a.open("Cameras and flash", "camera", box, new Runnable() {
                    @Override
                    public void run() {
                        stopCamera(cam);
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------- microphone and speaker

    static void play(short[] s, int rate, int channels) {
        int cfg = channels == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, rate, cfg,
                AudioFormat.ENCODING_PCM_16BIT, s.length * 2, AudioTrack.MODE_STATIC);
        t.write(s, 0, s.length);
        t.play();
        try {
            Thread.sleep((long) s.length * 1000L / (rate * channels) + 150);
        } catch (InterruptedException ignored) {
        }
        t.release();
    }

    static void mic(final MainActivity a) {
        a.withPerm(new String[]{Manifest.permission.RECORD_AUDIO}, true, new Runnable() {
            @Override
            public void run() {
                LinearLayout c = col(a);
                final TextView t = a.tv("Speak or tap the phone. The bar should move.", 15, MainActivity.FG);
                final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(100);
                final boolean[] runFlag = {true};
                final boolean[] want = {false};
                Button rec = a.btn("Record 3 seconds, then play back", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        want[0] = true;
                    }
                });
                c.addView(t);
                c.addView(bar);
                c.addView(rec);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int rate = 44100;
                        AudioRecord r = null;
                        try {
                            int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT);
                            r = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096) * 2);
                            if (r.getState() != AudioRecord.STATE_INITIALIZED) {
                                say("The microphone could not start");
                                return;
                            }
                            r.startRecording();
                            short[] buf = new short[2048];
                            short[] clip = new short[rate * 3];
                            int fill = -1;
                            while (runFlag[0]) {
                                int n = r.read(buf, 0, buf.length);
                                if (n < 0) break;
                                int peak = 0;
                                for (int i = 0; i < n; i++) peak = Math.max(peak, Math.abs((int) buf[i]));
                                final int pct = peak * 100 / 32768;
                                a.ui.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        bar.setProgress(pct);
                                    }
                                });
                                if (want[0]) {
                                    want[0] = false;
                                    fill = 0;
                                    say("Recording...");
                                }
                                if (fill >= 0) {
                                    int k = Math.min(n, clip.length - fill);
                                    System.arraycopy(buf, 0, clip, fill, k);
                                    fill += k;
                                    if (fill >= clip.length) {
                                        fill = -1;
                                        say("Playing back...");
                                        play(clip, rate, 1);
                                        say("Done. Speak or tap the phone.");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            say("Microphone error: " + e.getMessage());
                        } finally {
                            if (r != null) {
                                try {
                                    r.stop();
                                } catch (Exception ignored) {
                                }
                                r.release();
                            }
                        }
                    }

                    void say(final String s) {
                        a.ui.post(new Runnable() {
                            @Override
                            public void run() {
                                t.setText(s);
                            }
                        });
                    }
                }).start();

                a.open("Microphone", "mic", wrap(a, c), new Runnable() {
                    @Override
                    public void run() {
                        runFlag[0] = false;
                    }
                });
            }
        });
    }

    static void tone(final int side) { // 0 left, 1 right, 2 both
        new Thread(new Runnable() {
            @Override
            public void run() {
                int rate = 44100;
                short[] s = new short[rate * 2];
                for (int i = 0; i < rate; i++) {
                    short v = (short) (Math.sin(2 * Math.PI * 440 * i / rate) * 12000);
                    if (side != 1) s[2 * i] = v;
                    if (side != 0) s[2 * i + 1] = v;
                }
                play(s, rate, 2);
            }
        }).start();
    }

    static void speaker(final MainActivity a) {
        AudioManager am = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
        LinearLayout c = col(a);
        c.addView(a.tv("Media volume: " + am.getStreamVolume(AudioManager.STREAM_MUSIC) + " of "
                + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                + "\nTurn it up first. You should hear a tone from the side you pick.", 15, MainActivity.MUTED));
        c.addView(a.btn("Play left", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tone(0);
            }
        }));
        c.addView(a.btn("Play right", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tone(1);
            }
        }));
        c.addView(a.btn("Play both", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tone(2);
            }
        }));
        a.open("Speaker", "speaker", wrap(a, c), null);
    }

    // ---------------------------------------------------------------- vibration

    static void vibe(final MainActivity a) {
        final Vibrator vib = (Vibrator) a.getSystemService(Context.VIBRATOR_SERVICE);
        LinearLayout c = col(a);
        c.addView(a.tv(vib != null && vib.hasVibrator() ? "A vibration motor was found." : "No vibration motor reported.",
                15, MainActivity.FG));
        c.addView(a.btn("Short", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vib != null) vib.vibrate(200);
            }
        }));
        c.addView(a.btn("Long", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vib != null) vib.vibrate(1000);
            }
        }));
        c.addView(a.btn("Pattern", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vib != null) vib.vibrate(new long[]{0, 200, 100, 200, 100, 600}, -1);
            }
        }));
        a.open("Vibration", "vibe", wrap(a, c), new Runnable() {
            @Override
            public void run() {
                if (vib != null) vib.cancel();
            }
        });
    }

    // ---------------------------------------------------------------- sensors

    static String vals(float[] v) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < v.length && i < 3; i++) s.append(String.format(Locale.US, "%.2f  ", v[i]));
        return s.toString();
    }

    static void sensors(final MainActivity a) {
        final SensorManager sm = (SensorManager) a.getSystemService(Context.SENSOR_SERVICE);
        final int[] types = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_LIGHT, Sensor.TYPE_PROXIMITY, Sensor.TYPE_PRESSURE, Sensor.TYPE_GRAVITY,
                Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR,
                Sensor.TYPE_RELATIVE_HUMIDITY, Sensor.TYPE_AMBIENT_TEMPERATURE};
        final String[] names = {"Accelerometer", "Gyroscope", "Magnetometer", "Light", "Proximity", "Pressure",
                "Gravity", "Linear accel", "Rotation", "Humidity", "Temperature"};
        final float[][] vals = new float[types.length][];
        final Sensor[] found = new Sensor[types.length];

        final SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                for (int i = 0; i < types.length; i++) {
                    if (e.sensor.getType() == types[i]) vals[i] = e.values.clone();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor s, int accuracy) {
            }
        };
        for (int i = 0; i < types.length; i++) {
            found[i] = sm.getDefaultSensor(types[i]);
            if (found[i] != null) sm.registerListener(l, found[i], SensorManager.SENSOR_DELAY_NORMAL);
        }

        LinearLayout c = col(a);
        final TextView t = a.tv("", 15, MainActivity.FG);
        c.addView(t);
        final int total = sm.getSensorList(Sensor.TYPE_ALL).size();
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                StringBuilder s = new StringBuilder(total + " sensors installed. Move, tilt and cover the phone.\n\n");
                for (int i = 0; i < types.length; i++) {
                    s.append(names[i]).append(": ");
                    if (found[i] == null) s.append("not present");
                    else if (vals[i] == null) s.append("waiting...");
                    else s.append(vals(vals[i]));
                    s.append('\n');
                }
                t.setText(s.toString());
                a.ui.postDelayed(this, 300);
            }
        };
        a.ui.post(tick);
        a.open("Sensors", "sens", wrap(a, c), new Runnable() {
            @Override
            public void run() {
                a.ui.removeCallbacks(tick);
                sm.unregisterListener(l);
            }
        });
    }

    // ---------------------------------------------------------------- location

    static void gps(final MainActivity a) {
        a.withPerm(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, true, new Runnable() {
            @Override
            public void run() {
                final LocationManager lm = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
                LinearLayout c = col(a);
                final TextView t = a.tv("", 15, MainActivity.FG);
                c.addView(t);
                boolean on = false;
                try {
                    on = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (Exception ignored) {
                }
                t.setText("GPS switched on: " + (on ? "yes" : "NO - turn Location on in settings")
                        + "\n\nWaiting for a fix. Go near a window or outside...");

                final LocationListener l = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        t.setText(String.format(Locale.US,
                                "Provider: %s\nLatitude: %.5f\nLongitude: %.5f\nAccuracy: %.0f m\nAltitude: %.0f m\nSpeed: %.1f m/s\nTime: %s",
                                loc.getProvider(), loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(),
                                loc.getAltitude(), loc.getSpeed(), new Date(loc.getTime()).toString()));
                    }

                    @Override
                    public void onStatusChanged(String p, int s, Bundle b) {
                    }

                    @Override
                    public void onProviderEnabled(String p) {
                    }

                    @Override
                    public void onProviderDisabled(String p) {
                    }
                };
                try {
                    List<String> all = lm.getAllProviders();
                    if (all.contains(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, l);
                    }
                    if (all.contains(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, l);
                    }
                } catch (Exception e) {
                    t.setText("Location error: " + e.getMessage());
                }
                a.open("Location / GPS", "gps", wrap(a, c), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            lm.removeUpdates(l);
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        });
    }

    // ---------------------------------------------------------------- SIM, eSIM, VoLTE

    static final String[] SIM_STATE = {"Unknown", "No SIM", "PIN required", "PUK required", "Network locked",
            "Ready", "Not ready", "Permanently disabled", "Card error", "Restricted"};
    static final String[] NET_TYPE = {"Unknown", "GPRS", "EDGE", "UMTS", "CDMA", "EVDO 0", "EVDO A", "1xRTT",
            "HSDPA", "HSUPA", "HSPA", "iDEN", "EVDO B", "LTE (4G)", "eHRPD", "HSPA+", "GSM", "TD-SCDMA",
            "IWLAN", "LTE-CA (4G+)", "5G NR"};
    static final String[] PHONE_TYPE = {"None", "GSM", "CDMA", "SIP"};

    static void sim(final MainActivity a) {
        a.withPerm(new String[]{Manifest.permission.READ_PHONE_STATE}, false, new Runnable() {
            @Override
            public void run() {
                TelephonyManager tm = (TelephonyManager) a.getSystemService(Context.TELEPHONY_SERVICE);
                PackageManager pm = a.getPackageManager();
                StringBuilder s = new StringBuilder();
                if (tm == null || tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                    s.append("This device has no phone radio.\n");
                } else {
                    s.append("Phone type: ").append(name(PHONE_TYPE, tm.getPhoneType())).append('\n');
                    s.append("SIM state: ").append(name(SIM_STATE, tm.getSimState())).append('\n');
                    s.append("SIM operator: ").append(tm.getSimOperatorName()).append(" (")
                            .append(tm.getSimOperator()).append(")\n");
                    s.append("Network: ").append(tm.getNetworkOperatorName()).append(" (")
                            .append(tm.getNetworkOperator()).append(")\n");
                    s.append("Roaming: ").append(tm.isNetworkRoaming() ? "yes" : "no").append('\n');
                    int type = 0;
                    try {
                        type = tm.getNetworkType();
                        s.append("Network type: ").append(name(NET_TYPE, type)).append('\n');
                        if (Build.VERSION.SDK_INT >= 24) {
                            s.append("Data type: ").append(name(NET_TYPE, tm.getDataNetworkType())).append('\n');
                            s.append("Voice type: ").append(name(NET_TYPE, tm.getVoiceNetworkType())).append('\n');
                        }
                    } catch (SecurityException e) {
                        s.append("Network type: allow the Phone permission to see this\n");
                    }
                    if (Build.VERSION.SDK_INT >= 23) {
                        s.append("SIM slots: ").append(tm.getPhoneCount()).append('\n');
                    }
                    if (Build.VERSION.SDK_INT >= 22) {
                        try {
                            s.append("Active SIMs: ")
                                    .append(SubscriptionManager.from(a).getActiveSubscriptionInfoCount()).append('\n');
                        } catch (Exception e) {
                            s.append("Active SIMs: allow the Phone permission to see this\n");
                        }
                    }
                }
                s.append("\neSIM\n");
                s.append("  eSIM chip: ").append(pm.hasSystemFeature("android.hardware.telephony.euicc") ? "yes" : "no").append('\n');
                if (Build.VERSION.SDK_INT >= 28) {
                    try {
                        EuiccManager em = (EuiccManager) a.getSystemService(Context.EUICC_SERVICE);
                        s.append("  eSIM enabled: ").append(em != null && em.isEnabled() ? "yes" : "no").append('\n');
                    } catch (Exception ignored) {
                    }
                }
                s.append("\nVoLTE / calling over LTE\n");
                s.append("  Phone supports IMS: ").append(pm.hasSystemFeature("android.hardware.telephony.ims") ? "yes" : "not declared").append('\n');
                s.append("  Android hides the live VoLTE status from apps, so use the Radio info button below.\n");

                LinearLayout c = col(a);
                c.addView(a.tv(s.toString(), 15, MainActivity.FG));
                c.addView(a.btn("Open radio info (shows IMS / VoLTE)", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[][] targets = {
                                {"com.android.phone", "com.android.phone.settings.RadioInfo"},
                                {"com.android.phone", "com.android.phone.RadioInfo"},
                                {"com.android.settings", "com.android.settings.RadioInfo"},
                        };
                        for (String[] t : targets) {
                            try {
                                a.startActivity(new Intent().setClassName(t[0], t[1]));
                                return;
                            } catch (Exception ignored) {
                            }
                        }
                        a.toast("Not available on this phone. Try dialling *#*#4636#*#*");
                    }
                }));
                a.open("SIM, eSIM, VoLTE", "sim", wrap(a, c), null);
            }
        });
    }

    // ---------------------------------------------------------------- Wi-Fi, Bluetooth, NFC

    static void net(final MainActivity a) {
        StringBuilder s = new StringBuilder();

        WifiManager wm = (WifiManager) a.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        s.append("Wi-Fi\n");
        if (wm == null) {
            s.append("  none\n");
        } else {
            s.append("  switched on: ").append(wm.isWifiEnabled() ? "yes" : "no").append('\n');
            WifiInfo wi = wm.getConnectionInfo();
            if (wi != null && wi.getNetworkId() != -1) {
                int ip = wi.getIpAddress();
                s.append("  network: ").append(wi.getSSID()).append('\n');
                s.append("  signal: ").append(wi.getRssi()).append(" dBm, speed ").append(wi.getLinkSpeed()).append(" Mbps\n");
                s.append("  address: ").append(String.format(Locale.US, "%d.%d.%d.%d",
                        ip & 255, (ip >> 8) & 255, (ip >> 16) & 255, (ip >> 24) & 255)).append('\n');
            } else {
                s.append("  not connected\n");
            }
        }

        ConnectivityManager cm = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        s.append("\nActive connection: ").append(ni == null ? "none" : ni.getTypeName() + " (" + ni.getState() + ")").append('\n');

        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        s.append("\nBluetooth\n");
        if (bt == null) {
            s.append("  none\n");
        } else {
            try {
                s.append("  switched on: ").append(bt.isEnabled() ? "yes" : "no").append('\n');
            } catch (Exception e) {
                s.append("  status hidden\n");
            }
            s.append("  low energy (BLE): ")
                    .append(a.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le") ? "yes" : "no").append('\n');
        }

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(a);
        s.append("\nNFC\n");
        if (nfc == null) s.append("  none\n");
        else s.append("  switched on: ").append(nfc.isEnabled() ? "yes" : "no").append('\n');

        LinearLayout c = col(a);
        c.addView(a.tv(s.toString(), 15, MainActivity.FG));
        final TextView status = a.tv("", 15, MainActivity.MUTED);
        c.addView(a.btn("Test internet", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("Testing...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String r;
                        long t0 = System.currentTimeMillis();
                        try {
                            HttpURLConnection h = (HttpURLConnection)
                                    new URL("http://connectivitycheck.gstatic.com/generate_204").openConnection();
                            h.setConnectTimeout(5000);
                            h.setReadTimeout(5000);
                            int code = h.getResponseCode();
                            h.disconnect();
                            r = (code == 204 ? "Internet works" : "Unexpected reply " + code)
                                    + " (" + (System.currentTimeMillis() - t0) + " ms)";
                        } catch (Exception e) {
                            r = "No internet: " + e;
                        }
                        final String out = r;
                        a.ui.post(new Runnable() {
                            @Override
                            public void run() {
                                status.setText(out);
                            }
                        });
                    }
                }).start();
            }
        }));
        c.addView(status);
        a.open("Wi-Fi, Bluetooth, NFC", "net", wrap(a, c), null);
    }

    // ---------------------------------------------------------------- battery

    static final String[] BATT_STATUS = {"?", "Unknown", "Charging", "Discharging", "Not charging", "Full"};
    static final String[] BATT_HEALTH = {"?", "Unknown", "Good", "Overheating", "Dead", "Over voltage", "Failure", "Cold"};

    static void showBattery(TextView t, Intent i) {
        int level = i.getIntExtra("level", -1);
        int scale = i.getIntExtra("scale", 100);
        int plug = i.getIntExtra("plugged", 0);
        String plugged = plug == 0 ? "no" : ((plug & 1) != 0 ? "AC " : "") + ((plug & 2) != 0 ? "USB " : "")
                + ((plug & 4) != 0 ? "wireless" : "");
        String tech = i.getStringExtra("technology");
        t.setText("Level: " + (level * 100 / Math.max(scale, 1)) + " %\n"
                + "Status: " + name(BATT_STATUS, i.getIntExtra("status", 0)) + "\n"
                + "Plugged in: " + plugged + "\n"
                + "Health: " + name(BATT_HEALTH, i.getIntExtra("health", 0)) + "\n"
                + "Temperature: " + i.getIntExtra("temperature", 0) / 10.0 + " C\n"
                + "Voltage: " + i.getIntExtra("voltage", 0) + " mV\n"
                + "Type: " + (tech == null ? "?" : tech) + "\n\n"
                + "Plug in and unplug the charger. This screen updates by itself.");
    }

    static void batt(final MainActivity a) {
        LinearLayout c = col(a);
        final TextView t = a.tv("", 16, MainActivity.FG);
        c.addView(t);
        final BroadcastReceiver rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent i) {
                showBattery(t, i);
            }
        };
        Intent sticky = a.registerReceiver(rcv, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (sticky != null) showBattery(t, sticky);
        a.open("Battery and charging", "batt", wrap(a, c), new Runnable() {
            @Override
            public void run() {
                a.unregisterReceiver(rcv);
            }
        });
    }

    // ---------------------------------------------------------------- buttons and headset

    static void keys(final MainActivity a) {
        LinearLayout c = col(a);
        final TextView k = a.tv("Press the volume keys or any other hardware button.\nBack leaves this test.", 18, MainActivity.FG);
        final TextView h = a.tv("\nHeadphones: unknown", 16, MainActivity.MUTED);
        c.addView(k);
        c.addView(h);
        final BroadcastReceiver rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent i) {
                boolean in = i.getIntExtra("state", 0) == 1;
                h.setText("\nHeadphones: " + (in ? "plugged in" + (i.getIntExtra("microphone", 0) == 1 ? " (with microphone)" : "")
                        : "unplugged"));
            }
        };
        a.registerReceiver(rcv, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        a.open("Buttons and headset", "keys", wrap(a, c), new Runnable() {
            @Override
            public void run() {
                a.unregisterReceiver(rcv);
            }
        });
        a.keyView = k;
        a.keyTrap = true;
    }
}
