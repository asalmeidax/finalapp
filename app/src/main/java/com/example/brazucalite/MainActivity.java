package com.example.brazucalite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "prefs";
    private static final String KEY_SOURCE_HASH = "source_hash";
    private static final String KEY_LAST_SYNC = "last_sync";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private CatalogAdapter adapter;
    private TextView status;
    private File cacheFile;
    private GridView grid;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        cacheFile = new File(getFilesDir(), "brazuca_catalog_cache.json");
        adapter = new CatalogAdapter(this);
        grid = findViewById(R.id.grid);
        grid.setAdapter(adapter);
        status = findViewById(R.id.status);

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { adapter.filter(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });

        grid.setOnItemClickListener((p, v, pos, id) -> openItem(adapter.getMediaItem(pos)));

        Button refresh = findViewById(R.id.btnRefresh);
        refresh.setOnClickListener(v -> syncBrazuca(true));

        loadImmediately();
        syncBrazuca(false);
    }

    private void openItem(MediaItemModel m) {
        if (!BrazucaSource.isDirectPlayable(m.streamUrl)) {
            new AlertDialog.Builder(this)
                    .setTitle(m.title)
                    .setMessage("O item foi importado da lista remota do add-on, mas a reprodução usa um resolvedor específico do Kodi ou uma página intermediária. Esta versão lite só abre URLs diretas compatíveis com Media3 (HLS/MP4/DASH).")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("title", m.title);
        i.putExtra("url", m.streamUrl);
        startActivity(i);
    }

    private void loadImmediately() {
        io.execute(() -> {
            try {
                String json = cacheFile.exists() ? readFile(cacheFile) : readAsset("catalog.json");
                List<MediaItemModel> list = JsonCatalog.parse(json);
                runOnUiThread(() -> {
                    adapter.setItems(list);
                    focusGrid();
                    status.setText(cacheFile.exists()
                            ? "Última lista salva • " + list.size() + " itens • verificando atualização…"
                            : "Catálogo inicial • verificando fonte Brazuca…");
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Carregando fonte remota…"));
            }
        });
    }

    private void syncBrazuca(boolean manual) {
        if (manual) status.setText("Atualizando diretamente das listas remotas…");
        io.execute(() -> {
            try {
                BrazucaSource.Result result = BrazucaSource.download();
                if (result.items.isEmpty()) throw new Exception("a fonte retornou zero itens");

                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                String oldHash = sp.getString(KEY_SOURCE_HASH, "");
                boolean changed = !result.hash.equals(oldHash) || !cacheFile.exists();

                if (changed) {
                    writeFile(cacheFile, BrazucaCache.encode(result.items));
                    sp.edit()
                            .putString(KEY_SOURCE_HASH, result.hash)
                            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                            .apply();
                    runOnUiThread(() -> {
                        adapter.setItems(result.items);
                        focusGrid();
                        status.setText("Lista Brazuca atualizada automaticamente • " + result.items.size() + " itens");
                    });
                } else {
                    sp.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
                    if (manual) runOnUiThread(() -> status.setText("As listas remotas não mudaram • " + result.items.size() + " itens"));
                    else runOnUiThread(() -> status.setText("Lista sincronizada • " + result.items.size() + " itens"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Fonte remota indisponível; usando a última lista salva. " + shortMessage(e)));
            }
        });
    }

    private void focusGrid() {
        if (adapter.getCount() > 0) { grid.setSelection(0); grid.requestFocus(); }
    }

    private String shortMessage(Exception e) {
        String s = e.getMessage();
        if (s == null) return "";
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
    }

    private String readAsset(String name) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(name)));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close(); return sb.toString();
    }

    private String readFile(File f) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close(); return sb.toString();
    }

    private void writeFile(File f, String s) throws Exception {
        FileOutputStream out = new FileOutputStream(f);
        out.write(s.getBytes("UTF-8")); out.close();
    }
}
