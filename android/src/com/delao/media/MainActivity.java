package com.delao.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home screen of tiles. Each tile opens the matching app installed on the TV.
 * A tile finds its app by name; if it cannot, it asks once and remembers.
 */
public class MainActivity extends Activity {

    private static final int PER_ROW = 5;

    static final class Service {
        final String name;
        final int colorA;
        final int colorB;
        final String[] exact;
        final String[] contains;
        final String[] exclude;
        final String url;

        Service(String name, int colorA, int colorB, String[] exact, String[] contains,
                String[] exclude, String url) {
            this.name = name;
            this.colorA = colorA;
            this.colorB = colorB;
            this.exact = exact;
            this.contains = contains;
            this.exclude = exclude;
            this.url = url;
        }
    }

    static final class App {
        final String label;
        final String pkg;

        App(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    private static String[] s(String... v) {
        return v;
    }

    private static final String[] NONE = new String[0];

    private static final Service[] SUBSCRIPTIONS = {
        new Service("Apple TV", 0xFF3A3A40, 0xFF0E0E11, s("apple tv", "apple tv+"), s("apple tv"), NONE, null),
        new Service("Netflix", 0xFFB3121D, 0xFF3D0509, NONE, s("netflix"), NONE, null),
        new Service("HBO Max", 0xFF5B2BD6, 0xFF170A45, s("max", "hbo max"), s("hbo max"), NONE, null),
        new Service("Prime Video", 0xFF1273C9, 0xFF0A2444, NONE, s("prime video"), NONE, null),
        new Service("Peacock", 0xFF8A6A12, 0xFF1C1A2E, NONE, s("peacock"), NONE, null),
        new Service("DirecTV Stream", 0xFF0F6F8F, 0xFF082A3A, NONE, s("directv"), NONE, null),
    };

    private static final Service[] FREE = {
        new Service("Tubi", 0xFF7A2BD0, 0xFF2A0B52, NONE, s("tubi"), NONE, null),
        new Service("Pluto TV", 0xFF3D3F52, 0xFF12131C, NONE, s("pluto"), NONE, null),
        new Service("Plex", 0xFFA86A08, 0xFF2B1C05, s("plex"), s("plex"), NONE, null),
        new Service("Kanopy", 0xFFA3352A, 0xFF2E0E0B, NONE, s("kanopy"), NONE, null),
        new Service("Hoopla", 0xFF1767B8, 0xFF0A2140, NONE, s("hoopla"), NONE, null),
    };

    private static final Service[] DOCS = {
        new Service("YouTube", 0xFFC0261C, 0xFF3A0B08, s("youtube"), s("youtube"),
                s("music", "kids", "youtube tv"), null),
        new Service("PBS", 0xFF1F5FAE, 0xFF0B1F3D, s("pbs"), s("pbs"), s("kids"), null),
        new Service("Internet Archive", 0xFF4A4D57, 0xFF15161B, NONE, NONE, NONE,
                "https://archive.org/details/movies"),
    };

    private static final Service[] MUSIC = {
        new Service("Spotify", 0xFF178A45, 0xFF06281A, NONE, s("spotify"), NONE, null),
        new Service("YouTube Music", 0xFFB81F3A, 0xFF330812, NONE, s("youtube music"), NONE, null),
    };

    private SharedPreferences prefs;
    private View firstTile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("tiles", MODE_PRIVATE);
        getWindow().getDecorView().setBackgroundColor(0xFF000000);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setClipToPadding(false);
        page.setPadding(dp(48), dp(30), dp(48), dp(40));
        scroll.addView(page, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("de la O's Media", 34, 0xFFF5F5F7, true);
        page.addView(title);

        addSection(page, "Subscriptions", SUBSCRIPTIONS);
        addSection(page, "Free Movies and TV", FREE);
        addSection(page, "Documentaries and Video", DOCS);
        addSection(page, "Music", MUSIC);

        TextView hint = text("Hold Select on a tile to choose a different app for it.", 12, 0xFFA1A1A8, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(26);
        page.addView(hint, hp);

        setContentView(scroll);
        if (firstTile != null) {
            firstTile.requestFocus();
        }
    }

    private void addSection(LinearLayout page, String heading, Service[] services) {
        TextView h = text(heading, 18, 0xFFF5F5F7, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.topMargin = dp(22);
        hp.bottomMargin = dp(4);
        page.addView(h, hp);

        LinearLayout row = null;
        for (int i = 0; i < services.length; i++) {
            if (i % PER_ROW == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                page.addView(row);
            }
            View tile = tile(services[i]);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(160), dp(90));
            tp.setMargins(0, dp(8), dp(16), dp(8));
            row.addView(tile, tp);
            if (firstTile == null) {
                firstTile = tile;
            }
        }
    }

    private View tile(final Service service) {
        final TextView t = text(service.name, 17, 0xFFFFFFFF, true);
        t.setGravity(Gravity.BOTTOM | Gravity.START);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setFocusable(true);
        t.setFocusableInTouchMode(false);
        t.setClickable(true);
        t.setLongClickable(true);

        final GradientDrawable rest = card(service, false);
        final GradientDrawable lit = card(service, true);
        t.setBackground(rest);

        t.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(hasFocus ? lit : rest);
                float scale = hasFocus ? 1.1f : 1f;
                v.animate().scaleX(scale).scaleY(scale).translationZ(hasFocus ? dp(12) : 0)
                        .setDuration(160).start();
            }
        });
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(service);
            }
        });
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (service.url != null) {
                    return false;
                }
                choose(service);
                return true;
            }
        });
        return t;
    }

    private GradientDrawable card(Service service, boolean focused) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {service.colorA, service.colorB});
        d.setCornerRadius(dp(14));
        if (focused) {
            d.setStroke(dp(3), 0xFFFFFFFF);
        }
        return d;
    }

    /** Opens the app for a tile: remembered choice, then a name match, then ask. */
    private void open(Service service) {
        if (service.url != null) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(service.url)));
            } catch (Exception e) {
                toast("No browser found. Install Silk from the Appstore.");
            }
            return;
        }
        String saved = prefs.getString(service.name, null);
        if (saved != null && launch(saved)) {
            return;
        }
        List<App> apps = installedApps();
        String found = match(service, apps);
        if (found != null && launch(found)) {
            return;
        }
        choose(service);
    }

    private boolean launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) {
            intent = pm.getLaunchIntentForPackage(pkg);
        }
        if (intent == null) {
            return false;
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void choose(final Service service) {
        final List<App> apps = installedApps();
        if (apps.isEmpty()) {
            toast("No apps found on this TV.");
            return;
        }
        CharSequence[] labels = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).label;
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose the app for " + service.name)
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String pkg = apps.get(which).pkg;
                        prefs.edit().putString(service.name, pkg).apply();
                        if (!launch(pkg)) {
                            toast("That app could not be opened.");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<App> installedApps() {
        PackageManager pm = getPackageManager();
        List<App> apps = new ArrayList<App>();
        Set<String> seen = new HashSet<String>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(category);
            List<ResolveInfo> found;
            try {
                found = pm.queryIntentActivities(query, 0);
            } catch (Exception e) {
                continue;
            }
            for (ResolveInfo info : found) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) {
                    continue;
                }
                CharSequence label = info.loadLabel(pm);
                apps.add(new App(label == null ? pkg : label.toString(), pkg));
            }
        }
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App a, App b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return apps;
    }

    private static String match(Service service, List<App> apps) {
        for (App app : apps) {
            String label = app.label.trim().toLowerCase(Locale.US);
            for (String e : service.exact) {
                if (label.equals(e)) {
                    return app.pkg;
                }
            }
        }
        for (App app : apps) {
            String label = app.label.toLowerCase(Locale.US);
            boolean excluded = false;
            for (String x : service.exclude) {
                if (label.contains(x)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) {
                continue;
            }
            for (String c : service.contains) {
                if (label.contains(c)) {
                    return app.pkg;
                }
            }
        }
        return null;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return t;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
