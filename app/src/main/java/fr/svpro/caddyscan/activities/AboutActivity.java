package fr.svpro.caddyscan.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import fr.svpro.caddyscan.R;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private static final String AGPL_URL    = "https://www.gnu.org/licenses/agpl-3.0.html";
    private static final String OFF_URL     = "https://world.openfoodfacts.org";
    private static final String GLIDE_URL   = "https://github.com/bumptech/glide";
    private static final String RETROFIT_URL= "https://square.github.io/retrofit/";
    private static final String ROOM_URL    = "https://developer.android.com/jetpack/androidx/releases/room";
    private static final String CAMERAX_URL = "https://developer.android.com/training/camerax";
    private static final String MLKIT_URL   = "https://developers.google.com/ml-kit/vision/barcode-scanning";
    private static final String OKHTTP_URL  = "https://square.github.io/okhttp/";
    private static final String GSON_URL    = "https://github.com/google/gson";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("À propos");
        }

        // Bouton licence AGPL
        MaterialButton btnLicence = findViewById(R.id.btnLicence);
        btnLicence.setOnClickListener(v -> openUrl(AGPL_URL));

        // Liens bibliothèques
        setupLink(R.id.btnOpenFoodFacts, OFF_URL);
        setupLink(R.id.btnCameraX,       CAMERAX_URL);
        setupLink(R.id.btnMlKit,         MLKIT_URL);
        setupLink(R.id.btnRetrofit,      RETROFIT_URL);
        setupLink(R.id.btnOkHttp,        OKHTTP_URL);
        setupLink(R.id.btnGson,          GSON_URL);
        setupLink(R.id.btnRoom,          ROOM_URL);
        setupLink(R.id.btnGlide,         GLIDE_URL);
    }

    private void setupLink(int viewId, String url) {
        findViewById(viewId).setOnClickListener(v -> openUrl(url));
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
