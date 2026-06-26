package fr.svpro.caddyscan.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.database.AppDatabase;
import fr.svpro.caddyscan.models.Product;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "extra_barcode";

    private ImageView ivProductImage;
    private TextView tvProductName, tvBrand, tvBarcode, tvQuantity,
            tvCategories, tvIngredients, tvScannedAt;
    private Chip chipNutriscore;
    private View layoutNutriscore, layoutBrand, layoutQuantity,
            layoutCategories, layoutIngredients;

    private String currentBarcode;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Reload product after edit
                    loadProduct(currentBarcode);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivProductImage   = findViewById(R.id.ivProductImage);
        tvProductName    = findViewById(R.id.tvProductName);
        tvBrand          = findViewById(R.id.tvBrand);
        tvBarcode        = findViewById(R.id.tvBarcode);
        tvQuantity       = findViewById(R.id.tvQuantity);
        tvCategories     = findViewById(R.id.tvCategories);
        tvIngredients    = findViewById(R.id.tvIngredients);
        tvScannedAt      = findViewById(R.id.tvScannedAt);
        chipNutriscore   = findViewById(R.id.chipNutriscore);
        layoutNutriscore = findViewById(R.id.layoutNutriscore);
        layoutBrand      = findViewById(R.id.layoutBrand);
        layoutQuantity   = findViewById(R.id.layoutQuantity);
        layoutCategories = findViewById(R.id.layoutCategories);
        layoutIngredients= findViewById(R.id.layoutIngredients);

        currentBarcode = getIntent().getStringExtra(EXTRA_BARCODE);
        if (currentBarcode != null) loadProduct(currentBarcode);
    }

    private void loadProduct(String barcode) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Product product = AppDatabase.getInstance(this).productDao().getProductByBarcode(barcode);
            runOnUiThread(() -> displayProduct(product));
            executor.shutdown();
        });
    }

    private void displayProduct(Product product) {
        if (product == null) return;

        setTitle(product.getDisplayName());
        tvProductName.setText(product.getDisplayName());
        tvBarcode.setText(product.getBarcode());

        setText(tvBrand,       product.getBrand(),       layoutBrand);
        setText(tvQuantity,    product.getQuantity(),    layoutQuantity);
        setText(tvCategories,  product.getCategories(),  layoutCategories);
        setText(tvIngredients, product.getIngredients(), layoutIngredients);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE);
        tvScannedAt.setText("Scanné le " + sdf.format(new Date(product.getScannedAt())));

        // Nutriscore
        if (product.getNutriscore() != null && !product.getNutriscore().isEmpty()) {
            layoutNutriscore.setVisibility(View.VISIBLE);
            String grade = product.getNutriscore().toUpperCase();
            chipNutriscore.setText("Nutri-score : " + grade);
            int color;
            switch (grade) {
                case "A": color = 0xFF038141; break;
                case "B": color = 0xFF85BB2F; break;
                case "C": color = 0xFFFECB02; break;
                case "D": color = 0xFFEE8100; break;
                case "E": color = 0xFFE63E11; break;
                default:  color = 0xFF9E9E9E; break;
            }
            chipNutriscore.setTextColor(color);
        } else {
            layoutNutriscore.setVisibility(View.GONE);
        }

        // Image
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_product_placeholder)
                    .centerCrop()
                    .into(ivProductImage);
        } else {
            ivProductImage.setImageResource(R.drawable.ic_product_placeholder);
        }
    }

    private void setText(TextView tv, String value, View layout) {
        if (value != null && !value.isEmpty()) {
            tv.setText(value);
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, EditProductActivity.class);
            intent.putExtra(EditProductActivity.EXTRA_BARCODE, currentBarcode);
            editLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
