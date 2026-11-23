package com.hkust.marketplace.models;

import com.hkust.marketplace.utils.PasswordUtils;

import java.time.LocalDateTime;

/**
 * Abstract account superclass.  The UML specialisations are {@link Buyer}, {@link Seller}
 * and {@link Admin}; each subclass fixes its {@link UserRole}.
 *
 * <p>Simple in-memory model (no persistence).  TODO: Replace with a real entity +
 * repository when backend is added.</p>
 */
public abstract class User {
    private String username;
    private String fullName;
    private String passwordHash; // Salted PBKDF2-HMAC-SHA256 hash
    private UserRole role;
    private String residenceHall;   // Buyer optional
    private String bio;             // Seller optional
    private String preferredMeetup; // Seller optional
    private boolean active = true;
    private LocalDateTime createdAt;
    private double averageRating;   // For sellers
    private int ratingCount;

    // Soft-delete state.  Accounts are never hard-deleted from the in-memory store so
    // that deals, chats and ratings keep a valid audit trail.
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;

    protected User() {
        this.createdAt = LocalDateTime.now();
    }

    protected User(String username, String fullName, String password, UserRole role) {
        this();
        this.username = username;
        this.fullName = fullName;
        // Passwords are stored as salted PBKDF2 hashes – never as plaintext.
        this.passwordHash = PasswordUtils.hash(password);
        this.role = role;
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getResidenceHall() { return residenceHall; }
    public void setResidenceHall(String residenceHall) { this.residenceHall = residenceHall; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getPreferredMeetup() { return preferredMeetup; }
    public void setPreferredMeetup(String preferredMeetup) { this.preferredMeetup = preferredMeetup; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    @Override
    public String toString() {
        return fullName + " (@" + username + ")";
    }
}
