package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.User;

/**
 * Simple session holder for the currently logged-in user.
 * TODO: Replace with proper JWT / session token handling when backend is added.
 */
public class SessionManager {
    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    /**
     * Ends the current session when it belongs to the given account.  Used when an
     * administrator deactivates or deletes an account: that user must not keep a
     * usable session.  (The demo only has one live session, so this is a no-op for
     * other accounts.)
     */
    public static void logoutIf(String username) {
        if (currentUser != null && username != null
                && currentUser.getUsername().equalsIgnoreCase(username)) {
            currentUser = null;
        }
    }

    /** True when the current user still exists and has not been deactivated/deleted. */
    public static boolean isSessionValid() {
        return currentUser != null && currentUser.isActive() && !currentUser.isDeleted();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isBuyer() {
        return currentUser != null && currentUser.getRole() == com.hkust.marketplace.models.UserRole.BUYER;
    }

    public static boolean isSeller() {
        return currentUser != null && currentUser.getRole() == com.hkust.marketplace.models.UserRole.SELLER;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == com.hkust.marketplace.models.UserRole.ADMIN;
    }
}
