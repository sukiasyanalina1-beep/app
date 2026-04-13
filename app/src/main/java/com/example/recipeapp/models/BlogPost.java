package com.example.recipeapp.models;

public class BlogPost {
    private String id;
    private String title;
    private String content;
    private String authorId;
    private String authorName;
    private String imageUrl;
    private long createdAt;

    public BlogPost() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    private java.util.List<String> likedBy = new java.util.ArrayList<>();
    public java.util.List<String> getLikedBy() { return likedBy != null ? likedBy : new java.util.ArrayList<>(); }
    public void setLikedBy(java.util.List<String> likedBy) { this.likedBy = likedBy; }

    public String getExcerpt() {
        if (content == null) return "";
        return content.length() > 120 ? content.substring(0, 120) + "…" : content;
    }
}