package fr.svpro.caddyscan.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import fr.svpro.caddyscan.models.Product;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products ORDER BY scannedAt DESC")
    List<Product> getAllProducts();

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    @Query("DELETE FROM products")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM products")
    int getCount();
}
