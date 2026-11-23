package com.hkust.marketplace.models;

/**
 * Buyer account type.  The role is fixed by the subclass, matching the UML
 * generalisation {@code User <|-- Buyer}.
 */
public class Buyer extends User {

    public Buyer(String username, String fullName, String password) {
        super(username, fullName, password, UserRole.BUYER);
    }
}
