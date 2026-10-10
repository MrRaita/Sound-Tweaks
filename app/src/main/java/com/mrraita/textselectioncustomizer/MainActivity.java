package com.mrraita.textselectioncustomizer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String PREFS = ConfigProvider.PREFS;
    private EditText priority;
    private EditText forceMain;
    private EditText forceOverflow;
    private EditText hide;
    private EditText include;
    private EditText exclude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Text Selection Toolbar Customizer");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Android'in standart metin seçimi araç çubuğundaki eylemleri sırala. "
                + "Değişikliklerin uygulanması için LSPosed'de modülü etkinleştirip hedef "
                + "uygulamaları kapsama al ve ilgili uygulamaları yeniden başlat.");
        intro.setTextSize(14);
        intro.setPadding(0, dp(8), 0, dp(14));
        root.addView(intro);

        priority = addField(root, "Öncelik sırası",
                "Her satıra bir kural. İlk eşleşen eylem önce gelir.",
                p.getString("priority_rules", "package:com.deepl.mobiletranslator"));
        forceMain = addField(root, "Ana satıra sabitle",
                "Eşleşen eylemler mümkün olduğunda ana satırda tutulur.",
                p.getString("force_main_rules", "package:com.deepl.mobiletranslator"));
        forceOverflow = addField(root, "Üç nokta menüsüne taşı",
                "Eşleşen eylemler her zaman taşan menüye gönderilir.",
                p.getString("force_overflow_rules", ""));
        hide = addField(root, "Gizlenecek eylemler",
                "Eşleşen eylemler bu araç çubuğunda gösterilmez.",
                p.getString("hide_rules", ""));
        include = addField(root, "Yalnızca bu uygulamalarda uygula (isteğe bağlı)",
                "Boş bırakılırsa bütün uygulamalara uygulanır. Paket adları satır satır.",
                p.getString("include_packages", ""));
        exclude = addField(root, "Hariç tutulan uygulamalar",
                "Paket adları satır satır.",
                p.getString("exclude_packages", "com.mrraita.textselectioncustomizer"));

        TextView syntax = new TextView(this);
        syntax.setText("Kural biçimleri:\n"
                + "package:com.deepl.mobiletranslator\n"
                + "activity:com.deepl.mobiletranslator/.MiniTranslatorActivity\n"
                + "title:DeepL\n"
                + "action:android.intent.action.PROCESS_TEXT\n"
                + "id:16908321\n\n"
                + "Önek yazılmazsa kural, eylem başlığında büyük/küçük harfe duyarsız arama yapar. "
                + "Bir eylem hem ana satıra hem üç nokta menüsüne yazılırsa üç nokta kuralı kazanır.");
        syntax.setTextSize(13);
        syntax.setPadding(0, dp(10), 0, dp(16));
        root.addView(syntax);

        Button save = new Button(this);
        save.setText("Ayarları kaydet");
        save.setOnClickListener(v -> save());
        root.addView(save);

        Button reset = new Button(this);
        reset.setText("Varsayılanlara dön");
        reset.setOnClickListener(v -> {
            priority.setText("package:com.deepl.mobiletranslator");
            forceMain.setText("package:com.deepl.mobiletranslator");
            forceOverflow.setText("");
            hide.setText("");
            include.setText("");
            exclude.setText("com.mrraita.textselectioncustomizer");
            save();
        });
        root.addView(reset);

        setContentView(scroll);
    }

    private EditText addField(LinearLayout root, String label, String hint, String value) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(16);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setPadding(0, dp(8), 0, dp(4));
        root.addView(tv);

        EditText edit = new EditText(this);
        edit.setText(value);
        edit.setHint(hint);
        edit.setGravity(Gravity.TOP | Gravity.START);
        edit.setMinLines(2);
        edit.setMaxLines(6);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(edit);

        TextView help = new TextView(this);
        help.setText(hint);
        help.setTextSize(12);
        help.setPadding(0, 0, 0, dp(4));
        root.addView(help);
        return edit;
    }

    private void save() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("priority_rules", text(priority))
                .putString("force_main_rules", text(forceMain))
                .putString("force_overflow_rules", text(forceOverflow))
                .putString("hide_rules", text(hide))
                .putString("include_packages", text(include))
                .putString("exclude_packages", text(exclude))
                .apply();

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Throwable ignored) { }
        Toast.makeText(this, "Ayarlar kaydedildi. Hedef uygulamaları yeniden başlat.", Toast.LENGTH_LONG).show();
    }

    private String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
