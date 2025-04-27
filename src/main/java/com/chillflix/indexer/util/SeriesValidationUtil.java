package com.chillflix.indexer.util;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeriesValidationUtil {
    private final MeterRegistry meterRegistry;
    private static final String LOWERCASE_SHA_COUNTER = "series.sha.lowercase";
    private static final String INVALID_MAGNET_COUNTER = "series.magnet.invalid";
    private static final String BTIH_PREFIX = "urn:btih:";

    public boolean isValidSha256Hash(String hash) {
        if (hash == null) {
            log.warn("Hash validation failed: hash is null");
            return false;
        }

        // Verificar si contiene minúsculas
        if (hash.matches(".*[a-f].*")) {
            log.warn("Hash validation failed: lowercase characters found in hash '{}'", hash);
            meterRegistry.counter(LOWERCASE_SHA_COUNTER, 
                "hash", hash, 
                "timestamp", String.valueOf(System.currentTimeMillis()))
                .increment();
            return false;
        }

        log.debug("Hash validation successful for '{}'", hash);
        return true;
    }

    public String extractHashFromMagnet(String magnetLink) {
        if (magnetLink == null || magnetLink.isEmpty()) {
            log.warn("Magnet link validation failed: link is null or empty");
            meterRegistry.counter(INVALID_MAGNET_COUNTER, 
                "reason", "null_or_empty")
                .increment();
            return null;
        }

        try {
            // Buscar el segmento xt que contiene el hash
            int btihIndex = magnetLink.indexOf(BTIH_PREFIX);
            if (btihIndex == -1) {
                log.warn("Magnet link validation failed: no BTIH prefix found in '{}'", magnetLink);
                meterRegistry.counter(INVALID_MAGNET_COUNTER, 
                    "reason", "no_btih_prefix")
                    .increment();
                return null;
            }

            // Extraer el hash hasta el siguiente & o el final del string
            String hash = magnetLink.substring(btihIndex + BTIH_PREFIX.length());
            int endIndex = hash.indexOf('&');
            if (endIndex != -1) {
                hash = hash.substring(0, endIndex);
            }

            log.debug("Successfully extracted hash '{}' from magnet link", hash);
            return hash;
        } catch (Exception e) {
            log.error("Error processing magnet link: {}", e.getMessage(), e);
            meterRegistry.counter(INVALID_MAGNET_COUNTER, 
                "reason", "parsing_error",
                "error", e.getMessage())
                .increment();
            return null;
        }
    }
}