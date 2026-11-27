package br.com.tutoria.auth.controller;

import br.com.tutoria.auth.service.JwtService;
import br.com.tutoria.auth.util.TokenResponse;
import br.com.tutoria.auth.util.UsuarioLogin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de login: POST /autenticacao/login
 *
 * Usado por:
 * - Parte 4 (React): a tela de login envia {"usuario": "...", "senha": "..."}
 *   e recebe {"token": "..."}. Se usuário ou senha estiverem errados,
 *   recebe 401 (Unauthorized).
 */
@RestController
@RequestMapping("/autenticacao")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UsuarioLogin login) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.usuario(), login.senha()));

            String token = jwtService.generateAccessToken(authentication.getName());
            return ResponseEntity.ok(new TokenResponse(token));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}