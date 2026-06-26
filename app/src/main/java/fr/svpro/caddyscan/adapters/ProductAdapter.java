package fr.svpro.caddyscan.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import fr.svpro.caddyscan.R;
import fr.svpro.caddyscan.models.Product;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> products;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onProductEdit(Product product);
        void onProductDelete(Product product);
    }

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName, tvBrand, tvBarcode, tvDate;
        private final ImageButton btnEdit, btnDelete;
        private final View nutriscoreDot;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName  = itemView.findViewById(R.id.tvProductName);
            tvBrand        = itemView.findViewById(R.id.tvBrand);
            tvBarcode      = itemView.findViewById(R.id.tvBarcode);
            tvDate         = itemView.findViewById(R.id.tvDate);
            btnEdit        = itemView.findViewById(R.id.btnEdit);
            btnDelete      = itemView.findViewById(R.id.btnDelete);
            nutriscoreDot  = itemView.findViewById(R.id.nutriscoreDot);
        }

        void bind(Product product) {
            tvProductName.setText(product.getDisplayName());
            tvBarcode.setText(product.getBarcode());

            // Highlight "Produit inconnu" in orange to signal it needs editing
            boolean isUnknown = product.getName() == null || product.getName().isEmpty();
            tvProductName.setTextColor(itemView.getContext().getResources().getColor(
                    isUnknown ? R.color.warning : R.color.text_primary, null));

            if (product.getBrand() != null && !product.getBrand().isEmpty()) {
                tvBrand.setText(product.getBrand());
                tvBrand.setVisibility(View.VISIBLE);
            } else {
                tvBrand.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.FRANCE);
            tvDate.setText(sdf.format(new Date(product.getScannedAt())));

            // Nutriscore color dot
            if (product.getNutriscore() != null && !product.getNutriscore().isEmpty()) {
                nutriscoreDot.setVisibility(View.VISIBLE);
                int color = getNutriscoreColor(product.getNutriscore());
                nutriscoreDot.setBackgroundTintList(ColorStateList.valueOf(color));
            } else {
                nutriscoreDot.setVisibility(View.GONE);
            }

            // Product image
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrl())
                        .placeholder(R.drawable.ic_product_placeholder)
                        .error(R.drawable.ic_product_placeholder)
                        .centerCrop()
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.ic_product_placeholder);
            }

            itemView.setOnClickListener(v -> listener.onProductClick(product));
            btnEdit.setOnClickListener(v -> listener.onProductEdit(product));
            btnDelete.setOnClickListener(v -> listener.onProductDelete(product));
        }

        private int getNutriscoreColor(String grade) {
            switch (grade.toUpperCase()) {
                case "A": return 0xFF038141;
                case "B": return 0xFF85BB2F;
                case "C": return 0xFFFECB02;
                case "D": return 0xFFEE8100;
                case "E": return 0xFFE63E11;
                default:  return 0xFF9E9E9E;
            }
        }
    }
}
