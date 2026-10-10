package com.mrraita.textselectioncustomizer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public final class ConfigProvider extends ContentProvider {
    public static final String PREFS = "toolbar_settings";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!"get_config".equals(method) || getContext() == null) {
            return super.call(method, arg, extras);
        }

        SharedPreferences p = getContext().getSharedPreferences(PREFS, 0);
        Bundle out = new Bundle();
        out.putString("priority_rules", p.getString("priority_rules",
                "package:com.deepl.mobiletranslator"));
        out.putString("force_main_rules", p.getString("force_main_rules",
                "package:com.deepl.mobiletranslator"));
        out.putString("force_overflow_rules", p.getString("force_overflow_rules", ""));
        out.putString("hide_rules", p.getString("hide_rules", ""));
        out.putString("include_packages", p.getString("include_packages", ""));
        out.putString("exclude_packages", p.getString("exclude_packages",
                "com.mrraita.textselectioncustomizer"));
        return out;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                   String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Use call(get_config)");
    }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
