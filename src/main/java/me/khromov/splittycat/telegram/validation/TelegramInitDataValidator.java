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

public class TelegramInitDataValidator {

    private static final long AUTH_DATE_FUTURE_TOLERANCE_SECONDS = 600;

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
        long authDate = parseAuthDate(params);
        validateFreshness(authDate);
        validateHash(params);
        return parseUser(params);
    }

    private long parseAuthDate(Map<String, String> params) {
        try {
            return Long.parseLong(require(params, "auth_date"));
        } catch (NumberFormatException exception) {
            throw new TelegramInitDataValidationException("Auth date is invalid", exception);
        }
    }

    private void validateFreshness(long authDate) {
        long now = Instant.now().getEpochSecond();
        if (now + AUTH_DATE_FUTURE_TOLERANCE_SECONDS < authDate || now - authDate > maxAgeSeconds) {
            throw new TelegramInitDataValidationException("Auth date is invalid or too old");
        }
    }

    private void validateHash(Map<String, String> params) {
        String hashHex = require(params, "hash").toLowerCase(Locale.ROOT);
        byte[] expectedHash = calculateExpectedHash(buildDataCheckString(params));
        byte[] providedHash = hexToBytes(hashHex);

        if (!MessageDigest.isEqual(expectedHash, providedHash)) {
            throw new TelegramInitDataValidationException("Hash mismatch");
        }
    }

    private byte[] calculateExpectedHash(String dataCheckString) {
        byte[] secretKey = hmacSha256(
                "WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8)
        );
        return hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
    }

    private TelegramUserPayload parseUser(Map<String, String> params) {
        try {
            return objectMapper.readValue(require(params, "user"), TelegramUserPayload.class);
        } catch (Exception exception) {
            throw new TelegramInitDataValidationException("Failed to parse user data", exception);
        }
    }

    private static String buildDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String require(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new TelegramInitDataValidationException("Missing required parameter: " + key);
        }
        return value;
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new TelegramInitDataValidationException("Init data is empty");
        }

        Map<String, String> params = new HashMap<>();
        for (String part : query.split("&")) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = urlDecode(part.substring(0, separatorIndex));
            String value = urlDecode(part.substring(separatorIndex + 1));
            params.put(key, value);
        }
        return params;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new TelegramInitDataValidationException("Hash has invalid length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            int high = Character.digit(hex.charAt(index * 2), 16);
            int low = Character.digit(hex.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new TelegramInitDataValidationException("Hash contains invalid characters");
            }
            bytes[index] = (byte) ((high << 4) + low);
        }
        return bytes;
    }
}
