package com.example.aitraining.auth;

import com.example.aitraining.domain.Models.User;

public final class CurrentUserContext {
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static User require() {
        User user = CURRENT.get();
        if (user == null) {
            throw new UnauthorizedException("Authentication is required");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
