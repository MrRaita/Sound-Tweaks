package com.mrraita.textselectioncustomizer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;
import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "[TextSelectionToolbarCustomizer]";
    private static final String MODULE_PACKAGE = "com.mrraita.textselectioncustomizer";
    private static final Uri CONFIG_URI = Uri.parse(
            "content://com.mrraita.textselectioncustomizer.config");
    private static volatile Config cachedConfig;
    private static volatile long configReadAt;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) return;
        if (MODULE_PACKAGE.equals(lpparam.packageName)
                || "android".equals(lpparam.packageName)
                || "com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.internal.widget.floatingtoolbar.LocalFloatingToolbarPopup",
                    lpparam.classLoader,
                    "layoutMainPanelItems",
                    List.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object contextObject = XposedHelpers.getObjectField(
                                        param.thisObject, "mContext");
                                if (!(contextObject instanceof Context)) return;
                                Context context = (Context) contextObject;

                                Config config = readConfig(context);
                                if (!config.appliesTo(lpparam.packageName)) return;

                                Object raw = param.args[0];
                                if (!(raw instanceof List)) return;

                                List<?> incoming = (List<?>) raw;
                                ArrayList<MenuItem> items = new ArrayList<>();
                                for (Object obj : incoming) {
                                    if (!(obj instanceof MenuItem)) continue;
                                    MenuItem item = (MenuItem) obj;

                                    if (matchesAny(item, config.hideRules)) {
                                        continue;
                                    }

                                    boolean forceOverflow = matchesAny(item, config.forceOverflowRules);
                                    boolean forceMain = matchesAny(item, config.forceMainRules);

                                    // Overflow wins if a rule appears in both lists.
                                    try {
                                        if (forceOverflow) {
                                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                                        } else if (forceMain) {
                                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                                        }
                                    } catch (Throwable ignored) {
                                        // A non-standard MenuItem implementation may reject flag changes.
                                    }
                                    items.add(item);
                                }

                                // Stable priority ordering: matching earlier rules move to the front.
                                final List<String> priorityRules = config.priorityRules;
                                Collections.sort(items, new Comparator<MenuItem>() {
                                    @Override
                                    public int compare(MenuItem a, MenuItem b) {
                                        int ia = priorityIndex(a, priorityRules);
                                        int ib = priorityIndex(b, priorityRules);
                                        return Integer.compare(ia, ib);
                                    }
                                });

                                // Replace the argument instead of mutating a potentially immutable list.
                                param.args[0] = items;
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " hook error in " + lpparam.packageName + ": " + t);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + " could not hook " + lpparam.packageName + ": " + t);
        }
    }

    private static Config readConfig(Context context) {
        long now = SystemClock.uptimeMillis();
        Config local = cachedConfig;
        if (local != null && now - configReadAt < 1000) return local;

        synchronized (MainHook.class) {
            now = SystemClock.uptimeMillis();
            if (cachedConfig != null && now - configReadAt < 1000) return cachedConfig;
            try {
                ContentResolver resolver = context.getContentResolver();
                Bundle b = resolver.call(CONFIG_URI, "get_config", null, null);
                if (b != null) {
                    cachedConfig = new Config(
                            b.getString("priority_rules", "package:com.deepl.mobiletranslator"),
                            b.getString("force_main_rules", "package:com.deepl.mobiletranslator"),
                            b.getString("force_overflow_rules", ""),
                            b.getString("hide_rules", ""),
                            b.getString("include_packages", ""),
                            b.getString("exclude_packages", MODULE_PACKAGE));
                    configReadAt = now;
                    return cachedConfig;
                }
            } catch (Throwable t) {
                // Use safe defaults if the exported settings provider is unavailable.
                XposedBridge.log(TAG + " settings provider unavailable: " + t.getClass().getSimpleName());
            }

            cachedConfig = Config.defaults();
            configReadAt = now;
            return cachedConfig;
        }
    }

    private static int priorityIndex(MenuItem item, List<String> rules) {
        for (int i = 0; i < rules.size(); i++) {
            if (matches(item, rules.get(i))) return i;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean matchesAny(MenuItem item, List<String> rules) {
        for (String rule : rules) {
            if (matches(item, rule)) return true;
        }
        return false;
    }

    private static boolean matches(MenuItem item, String rawRule) {
        if (item == null || rawRule == null) return false;
        String rule = rawRule.trim();
        if (rule.isEmpty() || rule.startsWith("#")) return false;

        String lower = rule.toLowerCase(Locale.ROOT);
        String value = rule;
        String kind = "title";
        int colon = rule.indexOf(':');
        if (colon > 0) {
            kind = rule.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            value = rule.substring(colon + 1).trim();
        }
        if (value.isEmpty()) return false;

        try {
            if ("id".equals(kind)) {
                return item.getItemId() == Integer.parseInt(value);
            }

            Intent intent = item.getIntent();
            ComponentName component = intent == null ? null : intent.getComponent();
            String packageName = component != null ? component.getPackageName()
                    : (intent == null ? null : intent.getPackage());
            String activity = component == null ? null : component.flattenToShortString();
            String action = intent == null ? null : intent.getAction();
            String title = item.getTitle() == null ? "" : item.getTitle().toString();

            if ("package".equals(kind)) {
                return packageName != null && packageName.equalsIgnoreCase(value);
            }
            if ("activity".equals(kind)) {
                return activity != null && activity.equalsIgnoreCase(value);
            }
            if ("action".equals(kind)) {
                return action != null && action.equalsIgnoreCase(value);
            }
            if ("title".equals(kind)) {
                return title.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
            }

            // Unknown prefixes are treated as literal title searches for usability.
            return title.toLowerCase(Locale.ROOT).contains(lower);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<String> lines(String value) {
        ArrayList<String> out = new ArrayList<>();
        if (value == null) return out;
        for (String line : value.split("\\r?\\n")) {
            String s = line.trim();
            if (!s.isEmpty() && !s.startsWith("#")) out.add(s);
        }
        return out;
    }

    private static final class Config {
        final List<String> priorityRules;
        final List<String> forceMainRules;
        final List<String> forceOverflowRules;
        final List<String> hideRules;
        final List<String> includePackages;
        final List<String> excludePackages;

        Config(String priority, String forceMain, String forceOverflow, String hide,
               String include, String exclude) {
            priorityRules = lines(priority);
            forceMainRules = lines(forceMain);
            forceOverflowRules = lines(forceOverflow);
            hideRules = lines(hide);
            includePackages = lines(include);
            excludePackages = lines(exclude);
        }

        static Config defaults() {
            return new Config(
                    "package:com.deepl.mobiletranslator",
                    "package:com.deepl.mobiletranslator",
                    "", "", "", MODULE_PACKAGE);
        }

        boolean appliesTo(String packageName) {
            if (packageName == null) return false;
            for (String excluded : excludePackages) {
                if (packageName.equalsIgnoreCase(excluded)) return false;
            }
            if (includePackages.isEmpty()) return true;
            for (String included : includePackages) {
                if (packageName.equalsIgnoreCase(included)) return true;
            }
            return false;
        }
    }
}
