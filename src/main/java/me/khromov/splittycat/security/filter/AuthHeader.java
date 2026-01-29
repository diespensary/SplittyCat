package me.khromov.splittycat.security.filter;

import jakarta.servlet.http.HttpServletRequest;

final class AuthHeader {

    private AuthHeader() {
    }

    static String tokenWithPrefix(HttpServletRequest request, String prefix) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith(prefix)) {
            return null;
        }
        return auth.substring(prefix.length());
    }

    static String requiredHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException();
        }
        return v;
    }
}
