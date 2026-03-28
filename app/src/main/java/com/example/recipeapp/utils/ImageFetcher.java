package com.example.recipeapp.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageFetcher {

    private static final String TAG = "ImageFetcher";
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final OkHttpClient client = new OkHttpClient();

    public static void fetchAndUpdateImages() {
        Log.d(TAG, "fetchAndUpdateImages called");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("recipes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Found " + snapshot.size() + " recipes to process");
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String existingImage = doc.getString("imageUrl");
                        String docId = doc.getId();

                        Log.d(TAG, "Recipe: " + title + " | imageUrl: " + existingImage);

                        if (existingImage == null || existingImage.isEmpty()) {
                            fetchImageForTitle(title, docId, db);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to get recipes: " + e.getMessage()));
    }

    private static void fetchImageForTitle(String title, String docId, FirebaseFirestore db) {
        executor.execute(() -> {
            try {
                String query = title.trim().replace(" ", "%20");
                String url = "https://www.themealdb.com/api/json/v1/1/search.php?s=" + query;
                Log.d(TAG, "Fetching image for: " + title + " URL: " + url);

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.body() == null) {
                    Log.e(TAG, "Empty response for: " + title);
                    saveImage(docId, getFallbackImage(title), db, title);
                    return;
                }

                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray meals = json.optJSONArray("meals");

                if (meals != null && meals.length() > 0) {
                    String imageUrl = meals.getJSONObject(0).getString("strMealThumb");
                    Log.d(TAG, "Found image for " + title + ": " + imageUrl);
                    saveImage(docId, imageUrl, db, title);
                } else {
                    Log.d(TAG, "No match found for: " + title + " — using fallback");
                    saveImage(docId, getFallbackImage(title), db, title);
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception for: " + title + " — " + e.getMessage());
                saveImage(docId, getFallbackImage(title), db, title);
            }
        });
    }

    private static void saveImage(String docId, String imageUrl, FirebaseFirestore db, String title) {
        db.collection("recipes").document(docId)
                .update("imageUrl", imageUrl)
                .addOnSuccessListener(v -> Log.d(TAG, "Saved image for: " + title))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save image for: " + title + " — " + e.getMessage()));
    }

    private static String getFallbackImage(String title) {
        String t = title.toLowerCase();
        if (t.contains("chicken"))
            return "https://www.themealdb.com/images/media/meals/tyywsw1511461312.jpg";
        if (t.contains("beef") || t.contains("bolognese") || t.contains("stir"))
            return "https://www.themealdb.com/images/media/meals/svprys1511176755.jpg";
        if (t.contains("salmon") || t.contains("fish"))
            return "https://www.themealdb.com/images/media/meals/xxyupu1468262513.jpg";
        if (t.contains("pasta") || t.contains("spaghetti"))
            return "https://www.themealdb.com/images/media/meals/sutysw1468247559.jpg";
        if (t.contains("soup") || t.contains("lentil"))
            return "https://www.themealdb.com/images/media/meals/stpuws1511191310.jpg";
        if (t.contains("salad"))
            return "https://www.themealdb.com/images/media/meals/k29viq1585565980.jpg";
        if (t.contains("pancake"))
            return "https://www.themealdb.com/images/media/meals/rwuyqx1511383174.jpg";
        if (t.contains("brownie") || t.contains("chocolate"))
            return "https://www.themealdb.com/images/media/meals/wruvqv1511880994.jpg";
        if (t.contains("smoothie") || t.contains("oat") || t.contains("fruit"))
            return "https://www.themealdb.com/images/media/meals/sywswr1511383814.jpg";
        if (t.contains("egg"))
            return "https://www.themealdb.com/images/media/meals/1bsv1q1560459826.jpg";
        if (t.contains("curry"))
            return "https://www.themealdb.com/images/media/meals/vwrpps1503068729.jpg";
        if (t.contains("fries") || t.contains("potato"))
            return "https://www.themealdb.com/images/media/meals/1548772327.jpg";
        if (t.contains("hummus"))
            return "https://www.themealdb.com/images/media/meals/wuvryu1487237512.jpg";
        if (t.contains("apple") || t.contains("crumble"))
            return "https://www.themealdb.com/images/media/meals/xvsurr1511719182.jpg";
        if (t.contains("toast") || t.contains("avocado"))
            return "https://www.themealdb.com/images/media/meals/sywswr1511383814.jpg";
        if (t.contains("wrap"))
            return "https://www.themealdb.com/images/media/meals/tyywsw1511461312.jpg";
        if (t.contains("wing"))
            return "https://www.themealdb.com/images/media/meals/tyywsw1511461312.jpg";
        return "https://www.themealdb.com/images/media/meals/sutysw1468247559.jpg";
    }
}
