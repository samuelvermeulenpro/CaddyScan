package fr.svpro.caddyscan.utils;

import fr.svpro.caddyscan.models.OpenFoodFactsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OpenFoodFactsApi {

    @GET("api/v0/product/{barcode}.json?fields=product_name,product_name_fr,brands,quantity,image_url,categories,nutriscore_grade,ingredients_text,ingredients_text_fr")
    Call<OpenFoodFactsResponse> getProduct(@Path("barcode") String barcode);
}
