
package cl.duoc.campuslab.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureRolesConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {

        Collection<GrantedAuthority> authorities = extractRoles(jwt);

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
    }

    // Extraer los roles del token de Microsoft Entra ID
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {

        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeRole)
                .filter(Objects::nonNull)
                .distinct()
                .map(role ->
                        new SimpleGrantedAuthority("ROLE_" + role)
                )
                .collect(Collectors.toList());
    }

    // Adaptar nombres de Microsoft a los roles de CampusLab
    private String normalizeRole(String role) {

        String normalized = Normalizer.normalize(
                        role.trim(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        switch (normalized) {

            case "ESTUDIANTE":
                return "ESTUDIANTE";

            case "TECNICO":
                return "TECNICO";

            case "ADMIN":
            case "ADMINISTRADOR":
                return "ADMIN";

            case "AUDITOR":
                return "AUDITOR";

            default:
                return null;
        }
    }
}
