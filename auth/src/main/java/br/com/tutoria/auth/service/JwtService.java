package br.com.tutoria.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Gera e valida os tokens JWT.
 *
 * Usado por:
 * - AuthenticationController (este módulo): gera o token no login.
 * - JwtAuthenticationFilter (este módulo): valida o token em cada requisição.
 *
 * O "subject" do token é o nome do usuário (ex.: "desweb"). É esse valor
 * que o Gateway (Parte 2) usa como userId na PerguntaMessage.
 *
 * Propriedades exigidas no application.yml do Gateway:
 * - api.security.token.secret: chave secreta, com no mínimo 32 caracteres.
 * - jwt.security.accessTokenExpiration: validade do token, em segundos.
 */
@Service
public class JwtService {

    private final String secret;
    private final long accessTokenExpiration;

    public JwtService(@Value("${api.security.token.secret}") String secret,
                      @Value("${jwt.security.accessTokenExpiration}") long accessTokenExpiration) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(String usuario) {
        return Jwts.builder()
                .subject(usuario)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration * 1000))
                .signWith(chave())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsuarioFromToken(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}