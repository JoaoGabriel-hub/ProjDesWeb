package br.com.tutoria.auth.util;

/**
 * Resposta do login quando usuário e senha estão corretos.
 */
public record TokenResponse(
        String token
) {
}