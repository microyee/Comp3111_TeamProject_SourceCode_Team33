package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MarketplaceAccounts {
    public static final List<String> RESIDENCES = List.of("Hall 1", "Hall 2", "Hall 3", "Hall 4",
            "Hall 5", "Hall 6", "Hall 7", "Hall 8", "Hall 9", "UG Hall 1", "UG Hall 2", "Off-campus");

    private final DummyDataService data;

    public MarketplaceAccounts(DummyDataService data) {
        this.data = data;
    }

    public User authenticate(UserRole role, String username, String password) {
        String name = clean(username);
        require(!name.isEmpty() && !clean(password).isEmpty(), "Enter your username and password.");
        User user = data.findUser(name);
        require(user != null && PasswordUtils.verify(password, user.getPasswordHash()), "Incorrect username or password.");
        require(user.getRole() == role, "Use the portal for your account role.");
        require(available(user), "This account is inactive. Contact an administrator.");
        return user;
    }

    public User register(UserRole role, String username, String fullName, String password,
                         String confirm, String residence, String bio, String meetup) {
        synchronized (data) {
            String name = clean(username).toLowerCase(Locale.ROOT);
            require(role == UserRole.BUYER || role == UserRole.SELLER, "Choose Buyer or Seller.");
            require(name.matches("[a-z0-9][a-z0-9_.-]{2,31}"),
                    "Use 3–32 characters for your username, starting with a letter or number.");
            require(!data.usernameExists(name), "This username is already taken.");
            String displayName = requiredText(fullName, "Full name", 100);
            validatePassword(password, confirm);
            User user = role == UserRole.BUYER ? new Buyer(name, displayName, password)
                    : new Seller(name, displayName, password);
            applyDetails(user, residence, bio, meetup);
            data.registerUser(user);
            return user;
        }
    }

    public void updateProfile(User actor, String fullName, String residence, String bio, String meetup,
                              String current, String password, String confirm) {
        synchronized (data) {
            requireAccount(actor, actor == null ? null : actor.getRole());
            String displayName = requiredText(fullName, "Full name", 100);
            boolean changing = !clean(current).isEmpty() || !clean(password).isEmpty() || !clean(confirm).isEmpty();
            String hash = actor.getPasswordHash();
            if (changing) {
                require(PasswordUtils.verify(current, hash), "Current password is incorrect.");
                validatePassword(password, confirm);
                hash = PasswordUtils.hash(password);
            }
            applyDetails(actor, residence, bio, meetup);
            actor.setFullName(displayName);
            actor.setPasswordHash(hash);
            data.updateUser(actor);
        }
    }

    private void applyDetails(User user, String residence, String bio, String meetup) {
        if (user.getRole() == UserRole.BUYER) {
            require(residence == null || RESIDENCES.contains(residence), "Select a residence from the list.");
            user.setResidenceHall(residence);
        }
        if (user.getRole() == UserRole.SELLER) {
            String description = limitedText(bio, "Bio", 500);
            require(meetup == null || DummyDataService.HKUST_LOCATIONS.contains(meetup), "Select an HKUST meet-up location.");
            user.setBio(description);
            user.setPreferredMeetup(meetup);
        }
    }

    User requireAccount(User actor, UserRole role) {
        require(available(actor) && actor.getRole() == role && data.findUser(actor.getUsername()) == actor,
                "Sign in with an active account to continue.");
        return actor;
    }

    static boolean available(User user) {
        return user != null && user.isActive() && !user.isDeleted();
    }

    static void validatePassword(String password, String confirm) {
        String result = DummyDataService.checkPasswordStrength(password);
        require("OK".equals(result), result);
        require(Objects.equals(password, confirm), "Passwords do not match.");
    }

    public static String requiredText(String text, String label, int max) {
        String value = limitedText(text, label, max);
        require(!value.isEmpty(), label + " is required.");
        return value;
    }

    static String limitedText(String text, String label, int max) {
        String value = clean(text);
        require(value.length() <= max, label + " must be at most " + max + " characters.");
        return value;
    }

    public static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
