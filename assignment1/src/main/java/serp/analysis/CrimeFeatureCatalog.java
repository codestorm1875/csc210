package serp.analysis;

import serp.search.TextProcessor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Controlled vocabulary used to identify equivalent descriptions of a feature. */
public final class CrimeFeatureCatalog {
    private static final Map<String, Set<String>> FEATURES = createCatalog();

    public Set<String> detect(String content) {
        String normalized = " " + TextProcessor.normalize(content) + " ";
        Set<String> detected = new LinkedHashSet<>();
        FEATURES.forEach((feature, phrases) -> {
            boolean found = phrases.stream()
                    .map(TextProcessor::normalize)
                    .anyMatch(phrase -> normalized.contains(" " + phrase + " "));
            if (found) {
                detected.add(feature);
            }
        });
        return Set.copyOf(detected);
    }

    public Set<String> featureNames() {
        return FEATURES.keySet();
    }

    private static Map<String, Set<String>> createCatalog() {
        Map<String, Set<String>> catalog = new LinkedHashMap<>();
        catalog.put("Anonymous reporting", Set.of("anonymous report", "anonymous reporting", "anonymity", "anonymous mode"));
        catalog.put("GPS / geolocation mapping", Set.of("gps", "geolocation", "geotag", "map location", "crime map", "mapping"));
        catalog.put("Multimedia evidence", Set.of("photo evidence", "video evidence", "audio evidence", "multimedia evidence", "photo and video", "photo video and audio", "multimedia"));
        catalog.put("Real-time safety alerts", Set.of("real time alert", "push notification", "emergency alert", "location based push", "broadcasts real time"));
        catalog.put("Case status tracking", Set.of("status tracking", "case progress", "track case", "tracking replies"));
        catalog.put("Two-way communication", Set.of("two way", "feedback channel", "officer feedback", "exchanges two way"));
        catalog.put("Police / dispatch integration", Set.of("police integration", "law enforcement integration", "dispatch integration", "police dispatch", "police unit", "police portal"));
        catalog.put("Analytics and hotspot detection", Set.of("hotspot analytics", "hotspot chart", "dashboard analytics", "management dashboard"));
        catalog.put("Authentication and access control", Set.of("authentication", "authenticated", "verified identity", "access control"));
        catalog.put("Encryption and privacy", Set.of("encryption", "encrypted", "privacy preservation", "privacy control"));
        catalog.put("Multilingual support", Set.of("multilingual", "multiple local language", "local language"));
        catalog.put("Offline / SMS / USSD access", Set.of("offline", "sms", "ussd"));
        catalog.put("SOS / panic button", Set.of("sos", "panic button"));
        catalog.put("Community validation", Set.of("community validation", "crowdsourced verification", "moderator review"));
        catalog.put("Automatic classification", Set.of("automatic classification", "nlp classification", "routes incidents"));
        return Map.copyOf(catalog);
    }
}
