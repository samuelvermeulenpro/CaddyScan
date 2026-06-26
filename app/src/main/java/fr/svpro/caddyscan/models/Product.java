package fr.svpro.caddyscan.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "products")
public class Product {

    @PrimaryKey
    @NonNull
    private String barcode;

    private String name;
    private String brand;
    private String quantity;
    private String imageUrl;
    private String categories;
    private String nutriscore;
    private String ingredients;
    private long scannedAt;

    public Product(@NonNull String barcode) {
        this.barcode = barcode;
        this.scannedAt = System.currentTimeMillis();
    }

    // Getters & Setters
    @NonNull public String getBarcode() { return barcode; }
    public void setBarcode(@NonNull String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public String getNutriscore() { return nutriscore; }
    public void setNutriscore(String nutriscore) { this.nutriscore = nutriscore; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public long getScannedAt() { return scannedAt; }
    public void setScannedAt(long scannedAt) { this.scannedAt = scannedAt; }

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        return "Produit inconnu (" + barcode + ")";
    }
}
