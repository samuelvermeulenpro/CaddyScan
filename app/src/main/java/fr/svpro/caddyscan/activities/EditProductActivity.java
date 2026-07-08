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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProductActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "extra_barcode";

    private ImageView ivImagePreview;
    private TextInputEditText etName, etBrand, etQuantity, etCategories, etIngredients;
    private AutoCompleteTextView spinnerNutriscore;
    private MaterialButton btnSave, btnTakePhoto, btnPickGallery;
    private CircularProgressIndicator progressIndicator;

    private AppDatabase database;
    private ExecutorService executor;
    private Product currentProduct;

    /** URI temporaire pour la photo (doit survivre à la rotation) */
    private Uri pendingCameraUri;
    /** URI finale de l'image locale choisie (content:// ou file://) */
    private Uri selectedImageUri;

    // ── Launchers ──────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && pendingCameraUri != null) {
                    processPickedImage(pendingCameraUri);
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Persist read permission across process restarts
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
        setContentView(R.layout.activity_edit_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Modifier le produit");
        }

        ivImagePreview    = findViewById(R.id.ivImagePreview);
        etName            = findViewById(R.id.etName);
        etBrand           = findViewById(R.id.etBrand);
        etCategories      = findViewById(R.id.etCategories);
        etQuantity        = findViewById(R.id.etQuantity);
        etIngredients     = findViewById(R.id.etIngredients);
        spinnerNutriscore = findViewById(R.id.spinnerNutriscore);
        btnSave           = findViewById(R.id.btnSave);
        btnTakePhoto      = findViewById(R.id.btnTakePhoto);
        btnPickGallery    = findViewById(R.id.btnPickGallery);
        progressIndicator = findViewById(R.id.progressIndicator);

        spinnerNutriscore.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"", "A", "B", "C", "D", "E"}));

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        btnTakePhoto.setOnClickListener(v -> launchCamera());
        btnPickGallery.setOnClickListener(v -> galleryLauncher.launch(ImagePickerHelper.galleryIntent()));
        btnSave.setOnClickListener(v -> saveChanges());

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) loadProduct(barcode);
        else { Toast.makeText(this, "Erreur : produit introuvable", Toast.LENGTH_SHORT).show(); finish(); }
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

    // ── Data ───────────────────────────────────────────────────────────────

    private void loadProduct(String barcode) {
        progressIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            currentProduct = database.productDao().getProductByBarcode(barcode);
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                if (currentProduct != null) populateFields();
                else { Toast.makeText(this, "Produit introuvable", Toast.LENGTH_SHORT).show(); finish(); }
            });
        });
    }

    private void populateFields() {
        setField(etName,        currentProduct.getName());
        setField(etBrand,       currentProduct.getBrand());
        setField(etQuantity,    currentProduct.getQuantity());
        setField(etCategories,  currentProduct.getCategories());
        setField(etIngredients, currentProduct.getIngredients());

        String nutriscore = currentProduct.getNutriscore();
        spinnerNutriscore.setText(nutriscore != null ? nutriscore.toUpperCase() : "", false);

        // Afficher l'image existante (URL distante ou URI locale)
        String imageUrl = currentProduct.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop()
                    .placeholder(R.drawable.ic_product_placeholder)
                    .into(ivImagePreview);
        }
    }

    private void saveChanges() {
        if (currentProduct == null) return;

        String name = getText(etName);
        if (TextUtils.isEmpty(name)) {
            etName.setError("Le nom est requis");
            etName.requestFocus();
            return;
        }

        currentProduct.setName(name);
        currentProduct.setBrand(getText(etBrand));
        currentProduct.setQuantity(getText(etQuantity));
        currentProduct.setCategories(getText(etCategories));
        currentProduct.setIngredients(getText(etIngredients));

        // Image : priorité à l'image locale sélectionnée
        if (selectedImageUri != null) {
            currentProduct.setImageUrl(selectedImageUri.toString());
        }
        // Sinon l'URL existante est conservée

        String nutriscore = spinnerNutriscore.getText().toString().trim();
        currentProduct.setNutriscore(nutriscore.isEmpty() ? null : nutriscore);

        progressIndicator.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        executor.execute(() -> {
            database.productDao().update(currentProduct);
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(this, "Produit mis à jour !", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void setField(TextInputEditText f, String v) { f.setText(v != null ? v : ""); }
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
