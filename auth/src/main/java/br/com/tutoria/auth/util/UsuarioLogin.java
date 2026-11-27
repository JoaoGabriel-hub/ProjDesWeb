package br.com.tutoria.auth.util;

/**
 * Dados que o usuário envia para fazer login.
 */
public record UsuarioLogin(
        String usuario,
        String senha
) {
}