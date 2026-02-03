package me.khromov.splittycat.telegram.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.khromov.splittycat.telegram.dto.TelegramUserPayload;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelegramInitDataValidator {

    private static final Logger logger = LoggerFactory.getLogger(TelegramInitDataValidator.class);

    private final ObjectMapper objectMapper;
    private final String botToken;
    private final long maxAgeSeconds;

    public TelegramInitDataValidator(ObjectMapper objectMapper, String botToken, long maxAgeSeconds) {
        this.objectMapper = objectMapper;
        this.botToken = botToken;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public TelegramUserPayload validateAndExtractUser(String initData) {
        logger.info("Starting to validate initData: {}", initData);

        Map<String, String> params = parseQuery(initData);
        logger.debug("Parsed query parameters: {}", params);

        String hashHex = require(params, "hash").toLowerCase(Locale.ROOT);
        long authDate = parseLong(require(params, "auth_date"));
        long now = Instant.now().getEpochSecond();

        // Логирование для времени
        logger.debug("Current time: {} | authDate: {}", now, authDate);

        long tolerance = 600; // 10 минут
        if (now + tolerance < authDate || now - authDate > maxAgeSeconds) {
            logger.error("Auth date validation failed: now={} authDate={} tolerance={}", now, authDate, tolerance);
            throw new IllegalArgumentException("Auth date is invalid or too old.");
        }

        // Логирование для строки проверки
        String dataCheckString = params.entrySet().stream()
                .filter(e -> !"hash".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        logger.debug("Data check string: {}", dataCheckString);

        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));

        byte[] expected = hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
        byte[] provided = hexToBytes(hashHex);

        if (!MessageDigest.isEqual(expected, provided)) {
            logger.error("Hash mismatch: expected={} provided={}", expected, provided);
            throw new IllegalArgumentException("Hash mismatch");
        }

        String userJson = require(params, "user");
        try {
            logger.debug("User JSON: {}", userJson);
            return objectMapper.readValue(userJson, TelegramUserPayload.class);
        } catch (Exception e) {
            logger.error("Failed to parse user JSON", e);
            throw new IllegalArgumentException("Failed to parse user data");
        }
    }

    private static String require(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException();
        return v;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
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
