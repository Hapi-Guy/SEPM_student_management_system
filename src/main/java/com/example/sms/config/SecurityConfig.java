package com.example.sms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Role permissions:
 * <ul>
 *   <li>STUDENT - view the list of students</li>
 *   <li>TEACHER - view and edit students and teachers</li>
 *   <li>ADMIN   - view, add, edit and remove students and teachers</li>
 * </ul>
 * The API is stateless: every request sends the username and password with HTTP Basic auth.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] STUDENT_URLS = {"/api/students", "/api/students/**"};
    private static final String[] TEACHER_URLS = {"/api/teachers", "/api/teachers/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is for cookie-based sessions; this API has no session cookie
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Frontend files and error responses are public
                        .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.ico", "/error").permitAll()

                        .requestMatchers(HttpMethod.GET, STUDENT_URLS).hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, TEACHER_URLS).hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, STUDENT_URLS).hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, TEACHER_URLS).hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, STUDENT_URLS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, TEACHER_URLS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, STUDENT_URLS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, TEACHER_URLS).hasRole("ADMIN")

                        .anyRequest().authenticated())
                // Reply with a plain 401 instead of "WWW-Authenticate: Basic", which would make
                // the browser show its own login popup on top of our login form
                .httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
