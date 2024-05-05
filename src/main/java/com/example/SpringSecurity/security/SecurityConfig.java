package com.example.SpringSecurity.security;


import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.authorization.integration.jakarta.ServletPolicyEnforcerFilter;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.util.JsonSerialization;
import org.springframework.beans.factory.annotation.Value;;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {


//    private final PasswordEncoder passwordEncoder;

//    public SecurityConfig(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation("http://localhost:8080/realms/Derrick");
    }

    public static final String CONTEXT_PATH = "/api/v1/students";

    private static final String[] SWAGGER_ENDPOINTS = {

            "/", HttpMethod.GET.name(),
            "/actuator/**",
            CONTEXT_PATH + "/swagger-ui/**",
            CONTEXT_PATH + "/configuration/**",
            CONTEXT_PATH + "/swagger-resources/**",
            CONTEXT_PATH + "/swagger-ui.html/**",
            CONTEXT_PATH + "/api-docs/**",
            CONTEXT_PATH + "/webjars/**",
            CONTEXT_PATH +  "/assets/**",
            CONTEXT_PATH +  "/static/**"
    };

    @Value("${oauth.client.name}")
    private String keycloakClientName;

    /**
     * This method configures the security filter chain for the HTTP security.
     * It uses keycloak for securing rest api calls
     *
     * @param http The HTTP security configuration.
     * @return The configured security filter chain.
     * @throws Exception If an error occurs while configuring the security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(SWAGGER_ENDPOINTS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterAfter(createPolicyEnforcerFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    private ServletPolicyEnforcerFilter createPolicyEnforcerFilter() {
        return new ServletPolicyEnforcerFilter(new ConfigurationResolver() {
            @Override
            public PolicyEnforcerConfig resolve(HttpRequest httpRequest) {
                try {
                return JsonSerialization.readValue(getClass().getResourceAsStream("/policy-enforcer.json"), PolicyEnforcerConfig.class);
            } catch(IOException e) {
                throw new RuntimeException();
            }
        }
        });
}

    /**
     * This method is the Cors Configuration that allow Application CRUD on the server
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("HEAD",
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.applyPermitDefaultValues();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthorityConverter = jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> clientRoles = new ArrayList<>();
            if(realmAccess != null){
                Object realm_roles = realmAccess.get("roles");
                if(realm_roles != null){
                    clientRoles.addAll(((List<Object>) realm_roles)
                            .stream().map(Object::toString)
                            .toList());
                }
            }

            return clientRoles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        };
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthorityConverter);
        return jwtAuthenticationConverter;


    }



//    @Bean
//    protected UserDetailsService userDetailsService() {
//        UserDetails derrickUser = User.builder()
//                .username("biggestd")
//                .password(passwordEncoder.encode("12345"))
//                .roles("STUDENT")
//                .build();
//
//        return new InMemoryUserDetailsManager(
//                derrickUser
//        );
//
//    }



}
