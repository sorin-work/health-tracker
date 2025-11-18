package com.healthtracker.security;

/** The authenticated principal, taken from a verified JWT. */
public record AuthUser(Long id, String email) {}
