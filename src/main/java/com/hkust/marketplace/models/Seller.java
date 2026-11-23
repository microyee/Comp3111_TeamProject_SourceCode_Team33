package com.hkust.marketplace.models;

/**
 * Seller account type.  The role is fixed by the subclass, matching the UML
 * generalisation {@code User <|-- Seller}.
 */
public class Seller extends User {

    public Seller(String username, String fullName, String password) {
        super(username, fullName, password, UserRole.SELLER);
    }
}
