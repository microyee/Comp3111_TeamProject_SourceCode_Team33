package com.hkust.marketplace.models;

/**
 * Administrator account type.  The role is fixed by the subclass, matching the UML
 * generalisation {@code User <|-- Admin}.  Admin accounts are pre-provisioned and are
 * never created through public registration.
 */
public class Admin extends User {

    public Admin(String username, String fullName, String password) {
        super(username, fullName, password, UserRole.ADMIN);
    }
}
