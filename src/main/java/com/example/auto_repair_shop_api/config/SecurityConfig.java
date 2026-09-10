package com.example.auto_repair_shop_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(configurer ->
                        configurer
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/vehicles").hasRole("CUSTOMER")
                                .requestMatchers(HttpMethod.POST, "/api/service-requests").hasRole("CUSTOMER")
                                .requestMatchers(HttpMethod.PATCH, "/api/service-requests/{id}/approve").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/service-requests/{id}/reject").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/service-visits/{id}/parts").hasAnyRole("ADMIN", "MECHANIC")
                                .requestMatchers("/api/service-visits/**").hasRole("MECHANIC")
                                .requestMatchers(HttpMethod.GET, "/api/parts").hasAnyRole("ADMIN", "MECHANIC")
                                .requestMatchers(HttpMethod.POST, "/api/parts").hasRole("ADMIN")
                                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
