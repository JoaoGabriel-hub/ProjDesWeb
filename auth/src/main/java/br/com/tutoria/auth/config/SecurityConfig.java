package br.com.tutoria.auth.config;

import br.com.tutoria.auth.controller.AuthenticationController;
import br.com.tutoria.auth.filter.JwtAuthenticationFilter;
import br.com.tutoria.auth.service.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração de segurança do sistema.
 *
 * COMO USAR (Parte 2 - Gateway):
 *   na classe principal do Gateway, adicione:
 *
 *     @Import(SecurityConfig.class)
 *
 *   Isso traz junto o login (AuthenticationController), o filtro JWT e o
 *   JwtService, porque esta classe os importa logo abaixo.
 *
 * Regras:
 * - POST /autenticacao/login é público.
 * - Todo o resto exige o token JWT.
 * - CORS liberado para o React (http://localhost:5173).
 * - Usuários fixos em memória: desweb/desweb e desweb2/desweb2.
 */
@Configuration
@Import({JwtService.class, JwtAuthenticationFilter.class, AuthenticationController.class})
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        httpSecurity
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(c -> c.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(authorize -> authorize
                        // Necessário para o SSE: o envio da resposta acontece depois,
                        // numa etapa "assíncrona" da mesma requisição.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        .requestMatchers(HttpMethod.POST, "/autenticacao/login").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> {
                    ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json");
                        response.getWriter().write("""
                                {
                                    "status": 401,
                                    "message": "Necessario estar autenticado para acessar este recurso."
                                }
                                """);
                    });
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.getWriter().write("""
                                {
                                    "status": 403,
                                    "message": "Voce nao tem permissao para acessar este recurso."
                                }
                                """);
                    });
                });

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("desweb").password(passwordEncoder.encode("desweb")).roles("USER").build(),
                User.withUsername("desweb2").password(passwordEncoder.encode("desweb2")).roles("USER").build()
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}