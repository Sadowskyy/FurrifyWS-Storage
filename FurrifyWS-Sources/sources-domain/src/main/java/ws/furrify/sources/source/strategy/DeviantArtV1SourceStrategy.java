package ws.furrify.sources.source.strategy;

import ws.furrify.sources.keycloak.KeycloakServiceClient;
import ws.furrify.sources.keycloak.KeycloakServiceClientImpl;
import ws.furrify.sources.keycloak.PropertyHolder;
import ws.furrify.sources.providers.deviantart.DeviantArtScrapperClient;
import ws.furrify.sources.providers.deviantart.DeviantArtScrapperClientImpl;
import ws.furrify.sources.providers.deviantart.DeviantArtServiceClient;
import ws.furrify.sources.providers.deviantart.DeviantArtServiceClientImpl;
import ws.furrify.sources.providers.deviantart.dto.DeviantArtDeviationQueryDTO;
import ws.furrify.sources.providers.deviantart.dto.DeviantArtUserQueryDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Version 1 of Deviant Art Strategy.
 * Represents DeviantArt.com website for artists and content.
 *
 * @author sky
 */
public class DeviantArtV1SourceStrategy implements SourceStrategy {

    public final static String BROKER_ID = "deviantart";

    private final static String PROTOCOL = "https://";
    private final static String DOMAIN = "deviantart.com";
    private final static String WWW_SUBDOMAIN = "www.";
    private final static String DEVIATION_URL_FIELD = "url";
    private final static String USER_URL_FIELD = "url";
    private final static String DEVIATION_ART_PATH = "art";
    private final static byte DEVIATION_ART_PATH_POSITION_IN_URI = 2;
    private final static byte USERNAME_PATH_POSITION_IN_URI = 1;
    private final static byte DEVIATION_PATH_SEGMENTS = 4;
    private final static byte USER_PATH_SEGMENTS = 2;

    private final KeycloakServiceClient keycloakService;
    private final DeviantArtServiceClient deviantArtService;
    private final DeviantArtScrapperClient deviantArtScrapperClient;

    public DeviantArtV1SourceStrategy() {
        this.keycloakService = new KeycloakServiceClientImpl();
        this.deviantArtService = new DeviantArtServiceClientImpl();
        this.deviantArtScrapperClient = new DeviantArtScrapperClientImpl();
    }

    public DeviantArtV1SourceStrategy(final KeycloakServiceClient keycloakService,
                                      final DeviantArtServiceClient deviantArtService) {
        this.keycloakService = keycloakService;
        this.deviantArtService = deviantArtService;
        this.deviantArtScrapperClient = new DeviantArtScrapperClientImpl();
    }

    @Override
    public ValidationResult validateMedia(final HashMap<String, String> data) {
        if (data.get(DEVIATION_URL_FIELD) == null || data.get(DEVIATION_URL_FIELD).isBlank()) {
            return ValidationResult.invalid("Deviation url is required.");
        }

        URI uri;

        try {
            uri = new URI(data.get(DEVIATION_URL_FIELD));
            if (uri.getHost() == null) {
                throw new URISyntaxException(data.get(DEVIATION_URL_FIELD), "Domain is missing");
            }
        } catch (URISyntaxException e) {
            return ValidationResult.invalid("Deviation url is invalid.");
        }

        // If url does not contain correct domain
        if (!uri.getHost().replace(WWW_SUBDOMAIN, "").equalsIgnoreCase(DOMAIN)) {
            return ValidationResult.invalid("Deviation url is invalid.");
        }

        String[] path = uri.getPath().split("[/\\\\]");
        // If there are not enough path params in url or art path is present
        if (path.length < DEVIATION_PATH_SEGMENTS || !path[DEVIATION_ART_PATH_POSITION_IN_URI].equals(DEVIATION_ART_PATH)) {
            return ValidationResult.invalid("Deviation url is invalid.");
        }

        String deviationId;

        try {
            /* Extract deviation id using scrapper cause deviant art api
               is weird and doesn't allow getting deviation by id in url */
            deviationId = deviantArtScrapperClient.scrapDeviationId(PROTOCOL + DOMAIN + uri.getPath());
        } catch (IOException e) {
            return ValidationResult.invalid("Deviation not found.");
        }

        String providerBearerToken = "Bearer " + keycloakService.getKeycloakIdentityProviderToken(null, PropertyHolder.REALM, BROKER_ID).getAccessToken();

        DeviantArtDeviationQueryDTO deviationQueryDTO =
                deviantArtService.getDeviation(providerBearerToken, deviationId);
        if (deviationQueryDTO == null) {
            return ValidationResult.invalid("Deviation not found.");
        }

        // Save scrapped id to data
        data.put("deviation_id", deviationId);

        return ValidationResult.valid(data);
    }

    @Override
    public ValidationResult validateUser(final HashMap<String, String> data) {
        if (data.get(USER_URL_FIELD) == null || data.get(USER_URL_FIELD).isBlank()) {
            return ValidationResult.invalid("User url is required.");
        }

        URI uri;

        try {
            uri = new URI(data.get(DEVIATION_URL_FIELD));
            if (uri.getHost() == null) {
                throw new URISyntaxException(data.get(DEVIATION_URL_FIELD), "Domain is missing");
            }
        } catch (URISyntaxException e) {
            return ValidationResult.invalid("User url is invalid.");
        }

        // If url does not contain correct domain
        if (!uri.getHost().replace(WWW_SUBDOMAIN, "").equalsIgnoreCase(DOMAIN)) {
            return ValidationResult.invalid("User url is invalid.");
        }

        String[] path = uri.getPath().split("[/\\\\]");
        // If there are not enough path params
        if (path.length < USER_PATH_SEGMENTS) {
            return ValidationResult.invalid("User url is invalid.");
        }

        String providerBearerToken = "Bearer " + keycloakService.getKeycloakIdentityProviderToken(null, PropertyHolder.REALM, BROKER_ID).getAccessToken();

        DeviantArtUserQueryDTO userQueryDTO =
                deviantArtService.getUser(providerBearerToken, path[USERNAME_PATH_POSITION_IN_URI]);
        if (userQueryDTO == null) {
            return ValidationResult.invalid("User not found.");
        }

        data.put("user_id", userQueryDTO.getUser().getUserId());
        data.put("username", userQueryDTO.getUser().getUsername());

        return ValidationResult.valid(data);
    }

    @Override
    public ValidationResult validateAttachment(final HashMap<String, String> data) {
        return validateMedia(data);
    }
}
