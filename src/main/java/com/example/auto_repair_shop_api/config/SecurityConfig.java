package com.example.auto_repair_shop_api.config;

import com.example.auto_repair_shop_api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(configurer ->
                        configurer
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/vehicles").hasRole("CUSTOMER")
                                .requestMatchers(HttpMethod.POST, "/api/service-requests").hasRole("CUSTOMER")
                                .requestMatchers(HttpMethod.PATCH, "/api/service-requests/{id}/approve").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/service-requests/{id}/reject").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/service-visits/{id}/parts").hasAnyRole("ADMIN", "MECHANIC")
                                .requestMatchers(HttpMethod.PATCH, "/api/service-visits/{id}/invoice").hasRole("ADMIN")
                                .requestMatchers("/api/service-visits/**").hasRole("MECHANIC")
                                .requestMatchers(HttpMethod.GET, "/api/parts").hasAnyRole("ADMIN", "MECHANIC")
                                .requestMatchers(HttpMethod.POST, "/api/parts").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/invoices/{id}/pay").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/invoices").hasAnyRole("ADMIN", "CUSTOMER")
                                .anyRequest().authenticated()
                );


        return http.build();
    }
}
