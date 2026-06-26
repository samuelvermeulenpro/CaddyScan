package fr.svpro.caddyscan.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.database.AppDatabase;
import fr.svpro.caddyscan.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProductActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "extra_barcode";

    private TextInputEditText etName, etBrand, etQuantity, etImageUrl,
            etCategories, etIngredients;
    private AutoCompleteTextView spinnerNutriscore;
    private MaterialButton btnSave;
    private CircularProgressIndicator progressIndicator;

    private AppDatabase database;
    private ExecutorService executor;
    private Product currentProduct;

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

        etName        = findViewById(R.id.etName);
        etBrand       = findViewById(R.id.etBrand);
        etQuantity    = findViewById(R.id.etQuantity);
        etImageUrl    = findViewById(R.id.etImageUrl);
        etCategories  = findViewById(R.id.etCategories);
        etIngredients = findViewById(R.id.etIngredients);
        spinnerNutriscore = findViewById(R.id.spinnerNutriscore);
        btnSave       = findViewById(R.id.btnSave);
        progressIndicator = findViewById(R.id.progressIndicator);

        // Nutriscore dropdown
        String[] grades = {"", "A", "B", "C", "D", "E"};
        ArrayAdapter<String> nutriAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, grades);
        spinnerNutriscore.setAdapter(nutriAdapter);

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        String barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (barcode != null) {
            loadProduct(barcode);
        } else {
            Toast.makeText(this, "Erreur : produit introuvable", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadProduct(String barcode) {
        progressIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            currentProduct = database.productDao().getProductByBarcode(barcode);
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                if (currentProduct != null) {
                    populateFields();
                } else {
                    Toast.makeText(this, "Produit introuvable", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void populateFields() {
        setField(etName,        currentProduct.getName());
        setField(etBrand,       currentProduct.getBrand());
        setField(etQuantity,    currentProduct.getQuantity());
        setField(etImageUrl,    currentProduct.getImageUrl());
        setField(etCategories,  currentProduct.getCategories());
        setField(etIngredients, currentProduct.getIngredients());

        String nutriscore = currentProduct.getNutriscore();
        if (nutriscore != null && !nutriscore.isEmpty()) {
            spinnerNutriscore.setText(nutriscore.toUpperCase(), false);
        } else {
            spinnerNutriscore.setText("", false);
        }
    }

    private void setField(TextInputEditText field, String value) {
        field.setText(value != null ? value : "");
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
        currentProduct.setImageUrl(getText(etImageUrl));
        currentProduct.setCategories(getText(etCategories));
        currentProduct.setIngredients(getText(etIngredients));

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
