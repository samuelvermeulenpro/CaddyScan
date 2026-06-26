package fr.svpro.caddyscan.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import fr.svpro.caddyscan.models.Product;

@Database(entities = {Product.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    private static final String DATABASE_NAME = "barcode_scanner.db";

    public abstract ProductDao productDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
