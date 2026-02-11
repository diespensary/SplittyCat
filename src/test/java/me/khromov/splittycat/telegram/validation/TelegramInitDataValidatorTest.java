package me.khromov.splittycat.telegram.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.khromov.splittycat.telegram.dto.TelegramUserPayload;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "123456:ABC-TEST-TOKEN";

    @Test
    void validateAndExtractUser_returnsUser_whenInitDataSignatureIsValid() {
        TelegramInitDataValidator validator =
                new TelegramInitDataValidator(new ObjectMapper(), BOT_TOKEN, 86_400);

        long authDate = Instant.now().getEpochSecond();
        String userJson = "{\"id\":42,\"username\":\"splitty\",\"first_name\":\"Split\"}";
        String initData = buildInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "AAEAAAE",
                "user", userJson
        ));

        TelegramUserPayload payload = validator.validateAndExtractUser(initData);

        assertEquals(42L, payload.id());
        assertEquals("splitty", payload.username());
    }

    @Test
    void validateAndExtractUser_throws_whenHashIsInvalid() {
        TelegramInitDataValidator validator =
                new TelegramInitDataValidator(new ObjectMapper(), BOT_TOKEN, 86_400);

        long authDate = Instant.now().getEpochSecond();
        String userJson = "{\"id\":7,\"username\":\"cat\"}";
        String valid = buildInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "AAEINVALID",
                "user", userJson
        ));

        String tampered = valid.replace("hash=", "hash=deadbeef");

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateAndExtractUser(tampered));
    }



    @Test
    void validateAndExtractUser_throws_whenAuthDateTooOld() {
        TelegramInitDataValidator validator =
                new TelegramInitDataValidator(new ObjectMapper(), BOT_TOKEN, 60);

        long authDate = Instant.now().minusSeconds(3600).getEpochSecond();
        String userJson = "{\"id\":9,\"username\":\"old\"}";
        String initData = buildInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "OLD_QUERY",
                "user", userJson
        ));

        assertThrows(IllegalArgumentException.class,
                () -> validator.validateAndExtractUser(initData));
    }
    private static String buildInitData(Map<String, String> fields) {
        TreeMap<String, String> sorted = new TreeMap<>(fields);
        String dataCheckString = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8),
                BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        byte[] hash = hmacSha256(secret, dataCheckString.getBytes(StandardCharsets.UTF_8));
        String hashHex = toHex(hash);

        String encodedFields = sorted.entrySet().stream()
                .map(e -> url(e.getKey()) + "=" + url(e.getValue()))
                .collect(Collectors.joining("&"));

        return encodedFields + "&hash=" + hashHex;
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}