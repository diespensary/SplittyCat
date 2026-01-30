package me.khromov.splittycat.security.util;

import jakarta.servlet.http.HttpServletRequest;

public final class AuthHeader {
    public static String tokenWithPrefix(HttpServletRequest request, String prefix) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(prefix)) return null;
        return auth.substring(prefix.length());
    }
}
