
package com.chatwave.config;

import com.chatwave.repository.UserRepository;
import com.chatwave.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

 // Password encryption
 @Bean
 public PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder();
 }

 // CORS configuration for Angular frontend
 @Bean
 public CorsConfigurationSource corsConfigurationSource() {

  CorsConfiguration config = new CorsConfiguration();

  config.setAllowedOrigins(List.of(
          "http://localhost:4200",
          "http://127.0.0.1:4200"
  ));

  config.setAllowedMethods(List.of(
          "GET",
          "POST",
          "PUT",
          "PATCH",
          "DELETE",
          "OPTIONS"
  ));

  config.setAllowedHeaders(List.of(
          "Authorization",
          "Content-Type",
          "Accept",
          "Origin",
          "X-Requested-With"
  ));

  config.setExposedHeaders(List.of(
          "Authorization"
  ));

  config.setMaxAge(3600L);

  UrlBasedCorsConfigurationSource source =
          new UrlBasedCorsConfigurationSource();

  source.registerCorsConfiguration("/**", config);

  return source;
 }

 // Spring Security + JWT
 @Bean
 public SecurityFilterChain securityFilterChain(
         HttpSecurity http,
         JwtService jwt,
         UserRepository users
 ) throws Exception {

  return http

          .csrf(csrf -> csrf.disable())

          .cors(cors -> cors.configurationSource(
                  corsConfigurationSource()
          ))

          .sessionManagement(session ->
                  session.sessionCreationPolicy(
                          SessionCreationPolicy.STATELESS
                  )
          )

          .authorizeHttpRequests(auth -> auth

                  // Allow browser preflight requests
                  .requestMatchers(
                          HttpMethod.OPTIONS, "/**"
                  ).permitAll()

                  // Public authentication APIs
                  .requestMatchers(
                          "/api/auth/**",
                          "/ws/**",
                          "/error"
                  ).permitAll()

                  // Secure other APIs
                  .anyRequest().authenticated()
          )

          .addFilterBefore(
                  new OncePerRequestFilter() {

                   @Override
                   protected void doFilterInternal(
                           HttpServletRequest request,
                           HttpServletResponse response,
                           FilterChain filterChain
                   ) throws ServletException, IOException {

                    String header =
                            request.getHeader("Authorization");

                    if (header != null &&
                            header.startsWith("Bearer ")) {

                     try {
                      String token =
                              header.substring(7);

                      String email = jwt.email(token);

                      users.findByEmail(email)
                              .ifPresent(user -> {

                               var authentication =
                                       new UsernamePasswordAuthenticationToken(
                                               email,
                                               null,
                                               List.of()
                                       );

                               authentication.setDetails(
                                       new WebAuthenticationDetailsSource()
                                               .buildDetails(request)
                               );

                               SecurityContextHolder
                                       .getContext()
                                       .setAuthentication(
                                               authentication
                                       );
                              });

                     } catch (Exception ignored) {
                      SecurityContextHolder
                              .clearContext();
                     }
                    }

                    filterChain.doFilter(
                            request,
                            response
                    );
                   }
                  },
                  UsernamePasswordAuthenticationFilter.class
          )

          .build();
 }
}
