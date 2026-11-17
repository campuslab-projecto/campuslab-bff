
package cl.duoc.campuslab.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public AudienceValidator(String configuredAudience) {

        // Audiencias permitidas configuradas para CampusLab-API.
        // Se pueden separar por comas en application.yml.
        this.allowedAudiences = Arrays.stream(
                        configuredAudience.split(",")
                )
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        List<String> tokenAudiences = jwt.getAudience();

        boolean validAudience = tokenAudiences != null
                && tokenAudiences.stream()
                .anyMatch(allowedAudiences::contains);

        if (validAudience) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El token no está destinado a CampusLab-API.",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}
