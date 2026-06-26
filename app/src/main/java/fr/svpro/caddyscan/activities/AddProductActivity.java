package fr.svpro.caddyscan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.database.AppDatabase;
import fr.svpro.caddyscan.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddProductActivity extends AppCompatActivity {

    public static final String RESULT_BARCODE = "result_barcode";

    private TextInputLayout  tilBarcode;
    private TextInputEditText etBarcode, etName, etBrand, etQuantity,
            etImageUrl, etCategories, etIngredients;
    private AutoCompleteTextView spinnerNutriscore;
    private MaterialButton btnSave, btnScanBarcode;
    private CircularProgressIndicator progressIndicator;

    private AppDatabase database;
    private ExecutorService executor;

    /** Lance le scanner pour remplir le code-barres automatiquement */
    private final ActivityResultLauncher<Intent> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String barcode = result.getData().getStringExtra(ScannerActivity.EXTRA_BARCODE);
                    if (barcode != null) {
                        etBarcode.setText(barcode);
                        // Focus sur le nom pour continuer la saisie
                        etName.requestFocus();
                    }
                }
            });

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

        tilBarcode        = findViewById(R.id.tilBarcode);
        etBarcode         = findViewById(R.id.etBarcode);
        etName            = findViewById(R.id.etName);
        etBrand           = findViewById(R.id.etBrand);
        etQuantity        = findViewById(R.id.etQuantity);
        etImageUrl        = findViewById(R.id.etImageUrl);
        etCategories      = findViewById(R.id.etCategories);
        etIngredients     = findViewById(R.id.etIngredients);
        spinnerNutriscore = findViewById(R.id.spinnerNutriscore);
        btnSave           = findViewById(R.id.btnSave);
        btnScanBarcode    = findViewById(R.id.btnScanBarcode);
        progressIndicator = findViewById(R.id.progressIndicator);

        // Nutriscore dropdown
        String[] grades = {"", "A", "B", "C", "D", "E"};
        spinnerNutriscore.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, grades));

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        btnScanBarcode.setOnClickListener(v ->
                scannerLauncher.launch(new Intent(this, ScannerActivity.class)));

        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void saveProduct() {
        // Validation code-barres
        String barcode = getText(etBarcode);
        if (TextUtils.isEmpty(barcode)) {
            tilBarcode.setError("Le code-barres est requis");
            etBarcode.requestFocus();
            return;
        } else {
            tilBarcode.setError(null);
        }

        // Validation nom
        String name = getText(etName);
        if (TextUtils.isEmpty(name)) {
            etName.setError("Le nom du produit est requis");
            etName.requestFocus();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        executor.execute(() -> {
            // Vérifier doublon
            Product existing = database.productDao().getProductByBarcode(barcode);
            if (existing != null) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    tilBarcode.setError("Ce code-barres existe déjà dans la liste");
                    etBarcode.requestFocus();
                });
                return;
            }

            // Créer et insérer le produit
            Product product = new Product(barcode);
            product.setName(name);
            product.setBrand(getText(etBrand));
            product.setQuantity(getText(etQuantity));
            product.setImageUrl(getText(etImageUrl));
            product.setCategories(getText(etCategories));
            product.setIngredients(getText(etIngredients));

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

    private String getText(TextInputEditText field) {
        String val = field.getText() != null ? field.getText().toString().trim() : "";
        return val.isEmpty() ? null : val;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
