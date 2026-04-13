package com.example.recipeapp.models;

import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class Recipe {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String authorId;
    private String authorName;
    private String mealType;
    private List<String> equipment;
    private List<String> ingredients;
    private List<String> steps;
    private int prepTimeMinutes;
    private int cookTimeMinutes;
    private int servings;
    private boolean isUserCreated;
    private long createdAt;

    // Required empty constructor for Firestore
    public Recipe() {}

    public Recipe(String title, String description, String mealType,
                  List<String> equipment, List<String> ingredients,
                  List<String> steps, int prepTime, int cookTime, int servings) {
        this.title = title;
        this.description = description;
        this.mealType = mealType;
        this.equipment = equipment;
        this.ingredients = ingredients;
        this.steps = steps;
        this.prepTimeMinutes = prepTime;
        this.cookTimeMinutes = cookTime;
        this.servings = servings;
        this.isUserCreated = true;
        this.createdAt = System.currentTimeMillis();
    }

    // ── id ──────────────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // ── title ───────────────────────────────────────────────
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    // ── description ─────────────────────────────────────────
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ── imageUrl ────────────────────────────────────────────
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // ── authorId ────────────────────────────────────────────
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    // ── authorName ──────────────────────────────────────────
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    // ── mealType ────────────────────────────────────────────
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    // ── equipment ───────────────────────────────────────────
    @PropertyName("equipment")
    public List<String> getEquipment() { return equipment; }

    @PropertyName("equipment")
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }

    // ── ingredients ─────────────────────────────────────────
    @PropertyName("ingredients")
    public List<String> getIngredients() { return ingredients; }

    @PropertyName("ingredients")
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    // ── steps ───────────────────────────────────────────────
    @PropertyName("steps")
    public List<String> getSteps() { return steps; }

    @PropertyName("steps")
    public void setSteps(List<String> steps) { this.steps = steps; }

    // ── prepTimeMinutes ─────────────────────────────────────
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public void setPrepTimeMinutes(int prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }

    // ── cookTimeMinutes ─────────────────────────────────────
    public int getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(int cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }

    // ── servings ────────────────────────────────────────────
    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    // ── isUserCreated ───────────────────────────────────────
    @PropertyName("isUserCreated")
    public boolean isUserCreated() { return isUserCreated; }

    @PropertyName("isUserCreated")
    public void setUserCreated(boolean userCreated) { this.isUserCreated = userCreated; }

    // ── createdAt ───────────────────────────────────────────
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }


    // ── helper ──────────────────────────────────────────────
    public int getTotalTime() { return prepTimeMinutes + cookTimeMinutes; }
}