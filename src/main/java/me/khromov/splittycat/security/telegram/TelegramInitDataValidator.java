package me.khromov.splittycat.security.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TelegramInitDataValidator {

    private final ObjectMapper objectMapper;
    private final String botToken;
    private final long maxAgeSeconds;

    public TelegramInitDataValidator(ObjectMapper objectMapper, String botToken, long maxAgeSeconds) {
        this.objectMapper = objectMapper;
        this.botToken = botToken;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public TelegramUserPayload validateAndExtractUser(String initData) {
        Map<String, String> params = parseQuery(initData);

        String hashHex = params.get("hash");
        if (hashHex == null || hashHex.isBlank()) throw new IllegalArgumentException();
        hashHex = hashHex.toLowerCase(Locale.ROOT);

        String authDateRaw = params.get("auth_date");
        if (authDateRaw == null || authDateRaw.isBlank()) throw new IllegalArgumentException();

        long authDate;
        try {
            authDate = Long.parseLong(authDateRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        long now = Instant.now().getEpochSecond();
        if (now < authDate || now - authDate > maxAgeSeconds) throw new IllegalArgumentException();

        String dataCheckString = params.entrySet().stream()
                .filter(e -> !"hash".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256(
                "WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8)
        );

        byte[] expected = hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
        byte[] provided = hexToBytes(hashHex);

        if (!MessageDigest.isEqual(expected, provided)) throw new IllegalArgumentException();

        String userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) throw new IllegalArgumentException();

        try {
            return objectMapper.readValue(userJson, TelegramUserPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException();
        Map<String, String> map = new HashMap<>();
        for (String part : query.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = urlDecode(part.substring(0, idx));
            String val = urlDecode(part.substring(idx + 1));
            map.put(key, val);
        }
        return map;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) throw new IllegalArgumentException();
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException();
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
