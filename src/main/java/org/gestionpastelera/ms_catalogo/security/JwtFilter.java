package org.gestionpastelera.ms_catalogo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        try {
            final Claims claims = jwtUtil.extractAllClaims(token);
            if (jwtUtil.isTokenValid(claims) && SecurityContextHolder.getContext().getAuthentication() == null) {
                final String rut = jwtUtil.extractRutFromClaims(claims);
                final String rol = jwtUtil.extractRolFromClaims(claims);

                if (rut != null && !rut.isBlank()) {
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    if (rol != null && !rol.isBlank()) {
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            rut,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.debug("Token JWT inválido o expirado en ms-catalogo: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
