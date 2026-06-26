package fr.svpro.caddyscan.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.adapters.ProductAdapter;
import fr.svpro.caddyscan.database.AppDatabase;
import fr.svpro.caddyscan.models.OpenFoodFactsResponse;
import fr.svpro.caddyscan.models.Product;
import fr.svpro.caddyscan.utils.BackupManager;
import fr.svpro.caddyscan.utils.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private TextView tvEmpty;
    private CircularProgressIndicator progressIndicator;
    private AppDatabase database;
    private ExecutorService executor;

    // ── Activity Result Launchers ──────────────────────────────────────────

    private final ActivityResultLauncher<Intent> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String barcode = result.getData().getStringExtra(ScannerActivity.EXTRA_BARCODE);
                    if (barcode != null) fetchProductInfo(barcode);
                }
            });

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) loadProducts();
            });

    private final ActivityResultLauncher<Intent> addLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String barcode = result.getData().getStringExtra(AddProductActivity.RESULT_BARCODE);
                    loadProducts();
                    if (barcode != null) {
                        executor.execute(() -> {
                            Product p = database.productDao().getProductByBarcode(barcode);
                            if (p != null) runOnUiThread(() -> openProductDetail(p));
                        });
                    }
                }
            });

    /** SAF — choisir l'emplacement de sauvegarde (export) */
    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performExport(uri);
                }
            });

    /** SAF — choisir le fichier à importer */
    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performImport(uri);
                }
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView      = findViewById(R.id.recyclerView);
        tvEmpty           = findViewById(R.id.tvEmpty);
        progressIndicator = findViewById(R.id.progressIndicator);
        FloatingActionButton fabScan = findViewById(R.id.fabScan);

        database = AppDatabase.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        adapter = new ProductAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabScan.setOnClickListener(v -> scannerLauncher.launch(
                new Intent(this, ScannerActivity.class)));

        loadProducts();
    }

    // ── Data ───────────────────────────────────────────────────────────────

    private void loadProducts() {
        executor.execute(() -> {
            List<Product> products = database.productDao().getAllProducts();
            runOnUiThread(() -> {
                adapter.setProducts(products);
                updateEmptyState(products.isEmpty());
            });
        });
    }

    private void fetchProductInfo(String barcode) {
        progressIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            Product existing = database.productDao().getProductByBarcode(barcode);
            if (existing != null) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Produit déjà dans la liste", Toast.LENGTH_SHORT).show();
                    openProductDetail(existing);
                });
                return;
            }
            runOnUiThread(() ->
                RetrofitClient.getInstance().getApi().getProduct(barcode)
                    .enqueue(new Callback<OpenFoodFactsResponse>() {
                        @Override
                        public void onResponse(Call<OpenFoodFactsResponse> call,
                                               Response<OpenFoodFactsResponse> response) {
                            progressIndicator.setVisibility(View.GONE);
                            Product product = new Product(barcode);
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getStatus() == 1) {
                                OpenFoodFactsResponse.ProductData data = response.body().getProduct();
                                product.setName(data.getProductName());
                                product.setBrand(data.getBrands());
                                product.setQuantity(data.getQuantity());
                                product.setImageUrl(data.getImageUrl());
                                product.setCategories(data.getCategories());
                                product.setNutriscore(data.getNutriscoreGrade());
                                product.setIngredients(data.getIngredientsText());
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Produit non trouvé — vous pouvez le saisir manuellement",
                                        Toast.LENGTH_LONG).show();
                            }
                            saveProduct(product);
                        }
                        @Override
                        public void onFailure(Call<OpenFoodFactsResponse> call, Throwable t) {
                            progressIndicator.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this,
                                    "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            saveProduct(new Product(barcode));
                        }
                    })
            );
        });
    }

    private void saveProduct(Product product) {
        executor.execute(() -> {
            database.productDao().insert(product);
            List<Product> products = database.productDao().getAllProducts();
            runOnUiThread(() -> {
                adapter.setProducts(products);
                updateEmptyState(false);
                Toast.makeText(this, "Produit ajouté !", Toast.LENGTH_SHORT).show();
                if (product.getName() == null || product.getName().isEmpty()) {
                    openProductEdit(product);
                } else {
                    openProductDetail(product);
                }
            });
        });
    }

    // ── Export ─────────────────────────────────────────────────────────────

    private void startExport() {
        executor.execute(() -> {
            int count = database.productDao().getCount();
            runOnUiThread(() -> {
                if (count == 0) {
                    Toast.makeText(this, "La liste est vide, rien à exporter", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, BackupManager.generateFileName());
                exportLauncher.launch(intent);
            });
        });
    }

    private void performExport(Uri uri) {
        progressIndicator.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<Product> products = database.productDao().getAllProducts();
            BackupManager.exportToUri(this, uri, products, new BackupManager.ExportCallback() {
                @Override
                public void onSuccess(int count) {
                    runOnUiThread(() -> {
                        progressIndicator.setVisibility(View.GONE);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Export réussi ✓")
                                .setMessage(count + " produit(s) exporté(s) avec succès.\n\n"
                                        + "Le fichier JSON peut être partagé ou copié sur un autre appareil.")
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressIndicator.setVisibility(View.GONE);
                        showError("Erreur export", message);
                    });
                }
            });
        });
    }

    // ── Import ─────────────────────────────────────────────────────────────

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        // Fallback : certains gestionnaires n'associent pas .json à application/json
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/json", "text/plain", "*/*"
        });
        importLauncher.launch(intent);
    }

    private void performImport(Uri uri) {
        progressIndicator.setVisibility(View.VISIBLE);

        BackupManager.parseImport(this, uri, new BackupManager.ParseCallback() {
            @Override
            public void onSuccess(List<Product> incoming) {
                executor.execute(() -> {
                    // Récupérer les codes-barres existants
                    List<Product> existing = database.productDao().getAllProducts();
                    List<String> existingBarcodes = existing.stream()
                            .map(Product::getBarcode)
                            .collect(Collectors.toList());

                    int imported = 0;
                    int skipped  = 0;
                    for (Product p : incoming) {
                        if (existingBarcodes.contains(p.getBarcode())) {
                            skipped++;
                        } else {
                            database.productDao().insert(p);
                            imported++;
                        }
                    }

                    final int fImported = imported;
                    final int fSkipped  = skipped;
                    List<Product> updated = database.productDao().getAllProducts();

                    runOnUiThread(() -> {
                        progressIndicator.setVisibility(View.GONE);
                        adapter.setProducts(updated);
                        updateEmptyState(updated.isEmpty());

                        String msg = fImported + " produit(s) importé(s).";
                        if (fSkipped > 0) msg += "\n" + fSkipped + " doublon(s) ignoré(s).";

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Import réussi ✓")
                                .setMessage(msg)
                                .setPositiveButton("OK", null)
                                .show();
                    });
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    showError("Erreur import", message);
                });
            }
        });
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_BARCODE, product.getBarcode());
        startActivity(intent);
    }

    private void openProductEdit(Product product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra(EditProductActivity.EXTRA_BARCODE, product.getBarcode());
        editLauncher.launch(intent);
    }

    private void updateEmptyState(boolean isEmpty) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showError(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Adapter callbacks ──────────────────────────────────────────────────

    @Override
    public void onProductClick(Product product) { openProductDetail(product); }

    @Override
    public void onProductEdit(Product product) { openProductEdit(product); }

    @Override
    public void onProductDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer « " + product.getDisplayName() + " » de la liste ?")
                .setPositiveButton("Supprimer", (dialog, which) ->
                    executor.execute(() -> {
                        database.productDao().delete(product);
                        List<Product> products = database.productDao().getAllProducts();
                        runOnUiThread(() -> {
                            adapter.setProducts(products);
                            updateEmptyState(products.isEmpty());
                        });
                    })
                )
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Menu ───────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            addLauncher.launch(new Intent(this, AddProductActivity.class));
            return true;
        }

        if (id == R.id.action_export) {
            startExport();
            return true;
        }
        if (id == R.id.action_import) {
            new AlertDialog.Builder(this)
                    .setTitle("Importer une sauvegarde")
                    .setMessage("Les produits du fichier seront ajoutés à la liste existante. "
                            + "Les doublons (même code-barres) seront ignorés.")
                    .setPositiveButton("Choisir un fichier", (d, w) -> startImport())
                    .setNegativeButton("Annuler", null)
                    .show();
            return true;
        }
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        if (id == R.id.action_clear) {
            new AlertDialog.Builder(this)
                    .setTitle("Vider la liste")
                    .setMessage("Supprimer tous les produits ?")
                    .setPositiveButton("Tout supprimer", (d, w) ->
                        executor.execute(() -> {
                            database.productDao().deleteAll();
                            runOnUiThread(() -> {
                                adapter.setProducts(new ArrayList<>());
                                updateEmptyState(true);
                            });
                        })
                    )
                    .setNegativeButton("Annuler", null)
                    .show();
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
