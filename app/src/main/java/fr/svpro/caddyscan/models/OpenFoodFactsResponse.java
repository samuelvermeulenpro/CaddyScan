package fr.svpro.caddyscan.models;

import com.google.gson.annotations.SerializedName;

public class OpenFoodFactsResponse {

    @SerializedName("status")
    private int status;

    @SerializedName("product")
    private ProductData product;

    public int getStatus() { return status; }
    public ProductData getProduct() { return product; }

    public static class ProductData {
        @SerializedName("product_name")
        private String productName;

        @SerializedName("product_name_fr")
        private String productNameFr;

        @SerializedName("brands")
        private String brands;

        @SerializedName("quantity")
        private String quantity;

        @SerializedName("image_url")
        private String imageUrl;

        @SerializedName("categories")
        private String categories;

        @SerializedName("nutriscore_grade")
        private String nutriscoreGrade;

        @SerializedName("ingredients_text_fr")
        private String ingredientsTextFr;

        @SerializedName("ingredients_text")
        private String ingredientsText;

        public String getProductName() {
            if (productNameFr != null && !productNameFr.isEmpty()) return productNameFr;
            return productName;
        }
        public String getBrands() { return brands; }
        public String getQuantity() { return quantity; }
        public String getImageUrl() { return imageUrl; }
        public String getCategories() { return categories; }
        public String getNutriscoreGrade() { return nutriscoreGrade; }
        public String getIngredientsText() {
            if (ingredientsTextFr != null && !ingredientsTextFr.isEmpty()) return ingredientsTextFr;
            return ingredientsText;
        }
    }
}
