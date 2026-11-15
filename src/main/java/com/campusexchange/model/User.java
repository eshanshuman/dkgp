package com.campusexchange.model;

/**
 * A registered student.
 *
 * @param userId primary key from the users table
 * @param name   display name
 * @param email  unique login email
 */
public record User(int userId, String name, String email) {
}
