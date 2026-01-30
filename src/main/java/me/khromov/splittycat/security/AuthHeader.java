package me.khromov.splittycat.security;

import jakarta.servlet.http.HttpServletRequest;

final class AuthHeader {

    private AuthHeader() {}

    static String tokenWithPrefix(HttpServletRequest request, String prefix) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(prefix)) return null;
        return auth.substring(prefix.length());
    }
}
