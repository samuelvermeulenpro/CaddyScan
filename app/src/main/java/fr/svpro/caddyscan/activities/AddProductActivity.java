package fr.svpro.caddyscan.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.database.AppDatabase;
import fr.svpro.caddyscan.models.Product;
import fr.svpro.caddyscan.utils.ImagePickerHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    public static final String RESULT_BARCODE = "result_barcode";

    private ImageView ivImagePreview;
    private TextInputLayout tilBarcode;
    private TextInputEditText etBarcode, etName, etBrand, etQuantity, etCategories, etIngredients;
    private AutoCompleteTextView spinnerNutriscore;
    private MaterialButton btnSave, btnScanBarcode, btnTakePhoto, btnPickGallery;
    private CircularProgressIndicator progressIndicator;

    private AppDatabase database;
    private ExecutorService executor;
    private Uri pendingCameraUri;
    private Uri selectedImageUri;

    // ── Launchers ──────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String barcode = result.getData().getStringExtra(ScannerActivity.EXTRA_BARCODE);
                    if (barcode != null) { etBarcode.setText(barcode); etName.requestFocus(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && pendingCameraUri != null)
                    processPickedImage(pendingCameraUri);
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        processPickedImage(uri);
                    }
                }
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ajouter un produit");
        }

        ivImagePreview    = findViewById(R.id.ivImagePreview);
        tilBarcode        = findViewById(R.id.tilBarcode);
        etBarcode         = findViewById(R.id.etBarcode);
        etName            = findViewById(R.id.etName);
        etBrand           = findViewById(R.id.etBrand);
        etQuantity        = findViewById(R.id.etQuantity);
        etCategories      = findViewById(R.id.etCategories);
        etIngredients     = findViewById(R.id.etIngredients);
        spinnerNutriscore = findViewById(R.id.spinnerNutriscore);
        btnSave           = findViewById(R.id.btnSave);
        btnScanBarcode    = findViewById(R.id.btnScanBarcode);
        btnTakePhoto      = findViewById(R.id.btnTakePhoto);
        btnPickGallery    = findViewById(R.id.btnPickGallery);
        progressIndicator = findViewById(R.id.progressIndicator);

        spinnerNutriscore.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"", "A", "B", "C", "D", "E"}));

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        btnScanBarcode.setOnClickListener(v ->
                scannerLauncher.launch(new Intent(this, ScannerActivity.class)));
        btnTakePhoto.setOnClickListener(v -> launchCamera());
        btnPickGallery.setOnClickListener(v ->
                galleryLauncher.launch(ImagePickerHelper.galleryIntent()));
        btnSave.setOnClickListener(v -> saveProduct());
    }

    // ── Image picking ──────────────────────────────────────────────────────

    private void launchCamera() {
        try {
            pendingCameraUri = ImagePickerHelper.createCameraImageUri(this);
            cameraLauncher.launch(ImagePickerHelper.cameraIntent(pendingCameraUri));
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir l'appareil photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPickedImage(Uri sourceUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            Uri localUri = ImagePickerHelper.copyAndCompressImage(this, sourceUri);
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                if (localUri != null) {
                    selectedImageUri = localUri;
                    Glide.with(this).load(localUri).centerCrop().into(ivImagePreview);
                } else {
                    Toast.makeText(this, "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ── Save ───────────────────────────────────────────────────────────────

    private void saveProduct() {
        String barcode = getText(etBarcode);
        if (TextUtils.isEmpty(barcode)) {
            tilBarcode.setError("Le code-barres est requis"); etBarcode.requestFocus(); return;
        } else { tilBarcode.setError(null); }

        String name = getText(etName);
        if (TextUtils.isEmpty(name)) {
            etName.setError("Le nom du produit est requis"); etName.requestFocus(); return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        executor.execute(() -> {
            if (database.productDao().getProductByBarcode(barcode) != null) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    tilBarcode.setError("Ce code-barres existe déjà dans la liste");
                    etBarcode.requestFocus();
                });
                return;
            }

            Product product = new Product(barcode);
            product.setName(name);
            product.setBrand(getText(etBrand));
            product.setQuantity(getText(etQuantity));
            product.setCategories(getText(etCategories));
            product.setIngredients(getText(etIngredients));

            if (selectedImageUri != null) product.setImageUrl(selectedImageUri.toString());

            String nutriscore = spinnerNutriscore.getText().toString().trim();
            product.setNutriscore(nutriscore.isEmpty() ? null : nutriscore);

            database.productDao().insert(product);

            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(this, "Produit ajouté !", Toast.LENGTH_SHORT).show();
                Intent result = new Intent();
                result.putExtra(RESULT_BARCODE, barcode);
                setResult(RESULT_OK, result);
                finish();
            });
        });
    }

    private String getText(TextInputEditText f) {
        String v = f.getText() != null ? f.getText().toString().trim() : "";
        return v.isEmpty() ? null : v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }
}
