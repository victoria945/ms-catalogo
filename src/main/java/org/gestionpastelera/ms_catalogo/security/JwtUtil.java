package org.gestionpastelera.ms_catalogo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtUtil {

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*\\}");

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Value("${aws.cognito.region:us-east-1}")
    private String cognitoRegion;

    @Value("${aws.cognito.user-pool-id:}")
    private String cognitoUserPoolId;

    @Value("${aws.cognito.issuer-uri:}")
    private String cognitoIssuerUri;

    private volatile SecretKey cachedSigningKey;
    private final Map<String, PublicKey> jwksCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private SecretKey getSigningKey() {
        SecretKey key = this.cachedSigningKey;
        if (key == null) {
            synchronized (this) {
                key = this.cachedSigningKey;
                if (key == null) {
                    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
                    key = Keys.hmacShaKeyFor(keyBytes);
                    this.cachedSigningKey = key;
                }
            }
        }
        return key;
    }

    public Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token JWT vacío");
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new JwtException("Formato de token JWT inválido");
        }

        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String alg = extractJsonField(headerJson, "alg");
            String kid = extractJsonField(headerJson, "kid");

            // Validar token RS256 de Cognito
            if ("RS256".equalsIgnoreCase(alg) && !kid.isBlank()) {
                String issuer = resolveCognitoIssuer();
                if (issuer.isBlank()) {
                    throw new JwtException("AWS Cognito no está configurado para validar tokens RS256");
                }
                PublicKey publicKey = jwksCache.computeIfAbsent(kid, k -> fetchCognitoPublicKey(issuer, k));
                return Jwts.parser()
                        .verifyWith(publicKey)
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            }
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("No se pudo procesar el encabezado del token JWT: " + e.getMessage(), e);
        }

        // Validar token HS256 local
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String resolveCognitoIssuer() {
        if (cognitoIssuerUri != null && !cognitoIssuerUri.isBlank()) {
            return cognitoIssuerUri.trim().replaceAll("/+$", "");
        }
        if (cognitoUserPoolId != null && !cognitoUserPoolId.isBlank()) {
            String region = (cognitoRegion != null && !cognitoRegion.isBlank()) ? cognitoRegion.trim() : "us-east-1";
            return "https://cognito-idp." + region + ".amazonaws.com/" + cognitoUserPoolId.trim();
        }
        return "";
    }

    private PublicKey fetchCognitoPublicKey(String issuer, String kid) {
        try {
            String jwksUrl = issuer + "/.well-known/jwks.json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new JwtException("No se pudo obtener JWKS de AWS Cognito (HTTP " + response.statusCode() + ")");
            }
            Matcher objMatcher = JSON_OBJECT_PATTERN.matcher(response.body());
            while (objMatcher.find()) {
                String keyObj = objMatcher.group();
                if (kid.equals(extractJsonField(keyObj, "kid"))) {
                    String nStr = extractJsonField(keyObj, "n");
                    String eStr = extractJsonField(keyObj, "e");
                    byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
                    byte[] eBytes = Base64.getUrlDecoder().decode(eStr);
                    RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
                    return KeyFactory.getInstance("RSA").generatePublic(spec);
                }
            }
            throw new JwtException("Clave pública con kid=" + kid + " no encontrada en JWKS de Cognito");
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Error al obtener clave pública JWKS de Cognito: " + e.getMessage(), e);
        }
    }

    private String extractJsonField(String json, String fieldName) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractRut(String token) {
        return extractRutFromClaims(extractAllClaims(token));
    }

    public String extractRol(String token) {
        return extractRolFromClaims(extractAllClaims(token));
    }

    public String extractRutFromClaims(Claims claims) {
        if (claims == null) {
            return null;
        }
        String[] candidateKeys = {"custom:rut", "rut", "preferred_username", "cognito:username", "username"};
        for (String key : candidateKeys) {
            Object val = claims.get(key);
            if (val instanceof String str && !str.isBlank()) {
                return str.trim();
            }
        }
        String sub = claims.getSubject();
        return (sub != null && !sub.isBlank()) ? sub.trim() : null;
    }

    public String extractRolFromClaims(Claims claims) {
        if (claims == null) {
            return null;
        }
        String[] roleKeys = {"custom:rol", "rol"};
        for (String key : roleKeys) {
            Object val = claims.get(key);
            if (val instanceof String str && !str.isBlank()) {
                return normalizeRol(str);
            }
        }
        Object groups = claims.get("cognito:groups");
        if (groups instanceof Collection<?> groupList && !groupList.isEmpty()) {
            Object first = groupList.iterator().next();
            if (first != null && !first.toString().isBlank()) {
                return normalizeRol(first.toString());
            }
        }
        return null;
    }

    private String normalizeRol(String rawRol) {
        String clean = rawRol.trim();
        if (clean.toUpperCase().startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return clean.toUpperCase();
    }

    public boolean isTokenValid(Claims claims) {
        return claims != null && claims.getExpiration() != null && claims.getExpiration().after(new Date());
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return isTokenValid(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
