package cl.duoc.campuslab.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public AudienceValidator(String configuredAudience) {
        this.allowedAudiences = List.of(
                configuredAudience,
                "api://36ccc99d-6294-4333-a064-d62fa6237c7c",
                "36ccc99d-6294-4333-a064-d62fa6237c7c",
                "8902fa8d-4f71-4cd9-9a43-ddd5486e7327"
        );
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> tokenAudiences = jwt.getAudience();

        boolean validAudience = tokenAudiences.stream()
                .anyMatch(allowedAudiences::contains);

        if (validAudience) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no contiene una audiencia permitida. Audiencias recibidas: " + tokenAudiences,
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}