package com.campusexchange.model;

import java.time.LocalDate;

/**
 * One row of a user's borrow history (a requests row joined with its resource).
 *
 * @param requestId     primary key from the requests table
 * @param resourceId    the borrowed resource
 * @param title         resource title
 * @param category      resource category
 * @param requestDate   date the item was borrowed
 * @param currentStatus the resource's status right now
 */
public record BorrowRecord(int requestId, int resourceId, String title, String category,
                           LocalDate requestDate, String currentStatus) {
}
