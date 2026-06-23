package com.cloudShare.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    private final ClerkJwksProvider clerkJwksProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().contains("/api/v1/webhooks") || request.getRequestURI().contains("/api/v1/public/files")
        || request.getRequestURI().contains("/download") || request.getRequestURI().contains("/actuator/health") ) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authorization Header is Required");
            return;
        }
        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");
            if (chunks.length < 3) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid JWT token format");
                return;
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(chunks[0]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerJson);

            if (!headerNode.has("kid")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token is Missing KeyId");
                return;
            }
            String keyId = headerNode.get("kid").asText();

            PublicKey publicKey = clerkJwksProvider.getPublicKey(keyId);

//          Verify Token
            Claims claims = Jwts.parser()
                    .verifyWith((java.security.interfaces.RSAPublicKey) publicKey)
                    .requireIssuer(clerkIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String clerkId = claims.getSubject();
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(clerkId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invlaid Jwt Token" + ex.getMessage());
            return;
        }
        filterChain.doFilter(request, response);

    }
}
