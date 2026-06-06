package com.example.goldenhosewarehouse.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // تعطيل حماية CSRF للتجربة
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // السماح للجميع بالوصول لكل شيء
                );
        return http.build();
    }
}