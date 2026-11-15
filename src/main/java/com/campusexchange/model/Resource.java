package com.campusexchange.model;

/**
 * An item a student offers to lend.
 *
 * @param resourceId primary key from the resources table
 * @param title      what the item is, e.g. "Head First Java"
 * @param category   e.g. Books, Calculators, Notes
 * @param status     "Available" or "Borrowed"
 * @param ownerId    user_id of the lender
 * @param ownerName  lender's name (joined from users, for display)
 */
public record Resource(int resourceId, String title, String category, String status,
                       int ownerId, String ownerName) {

    public static final String AVAILABLE = "Available";
    public static final String BORROWED = "Borrowed";
}
