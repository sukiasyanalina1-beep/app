package com.example.recipeapp.models;

public class ShoppingItem {
    private String id;
    private String name;
    private boolean checked;
    private String addedByUserId;
    private String addedByName;
    private String category;
    private int quantity;
    private String unit;
    private long createdAt;

    public ShoppingItem() {}

    public ShoppingItem(String name, String addedByUserId, String addedByName) {
        this.name = name;
        this.addedByUserId = addedByUserId;
        this.addedByName = addedByName;
        this.checked = false;
        this.quantity = 1;
        this.unit = "";
        this.category = "other";
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public String getAddedByUserId() { return addedByUserId; }
    public void setAddedByUserId(String addedByUserId) { this.addedByUserId = addedByUserId; }
    public String getAddedByName() { return addedByName; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}