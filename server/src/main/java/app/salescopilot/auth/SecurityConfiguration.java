package app.salescopilot.auth;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ott.DefaultGenerateOneTimeTokenRequestResolver;
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenFilter;
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenRequestResolver;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import app.salescopilot.auth.api.CurrentUser;
import app.salescopilot.auth.domain.EmailAddresses;
import app.salescopilot.shared.Problems;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration implements WebMvcConfigurer {

    static final Duration LINK_VALIDITY = Duration.ofMinutes(15);

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserResolver());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JdbcOneTimeTokenService tokens,
            GoogleSignInHandler google, UserAccounts users, SignedInSession session, LoginMail mail, JsonMapper json,
            JdbcClient jdbc, TransactionTemplate transactions, Clock clock) {
        OneTimeTokenGenerationSuccessHandler sendLink = (request, response, token) -> {
            try {
                mail.sendLoginLink(token.getUsername(), token.getTokenValue());
            } catch (MailException e) {
                Problems.write(response, json, HttpStatus.SERVICE_UNAVAILABLE, "mail_unavailable",
                        "Não foi possível enviar o link. Tente de novo.", request.getRequestURI());
                return;
            }
            response.setStatus(HttpStatus.OK.value());
        };
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/ott/generate", "/login/ott", "/login/identity-link").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/login-links/*", "/api/identity-links/*").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.spa())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(new LoginLinkRequestLimiter(jdbc, transactions, clock, json), GenerateOneTimeTokenFilter.class)
                .oneTimeTokenLogin(ott -> ott
                        .tokenService(tokens)
                        .generateRequestResolver(linkValidity())
                        .tokenGenerationSuccessHandler(sendLink)
                        .showDefaultSubmitPage(false)
                        .authenticationSuccessHandler((request, response, authentication) -> {
                            CurrentUser user = users.findOrCreateByEmail(authentication.getName());
                            session.remember(request, user);
                            response.sendRedirect("/");
                        })
                        .authenticationFailureHandler((request, response, e) -> Problems.write(response, json,
                                HttpStatus.UNAUTHORIZED, "invalid_token", "Este link já foi usado ou expirou. Peça um novo.",
                                request.getRequestURI())))
                .oauth2Login(oauth -> oauth
                        .successHandler(google)
                        .failureHandler((request, response, e) -> response.sendRedirect("/entrar?erro=google_failed")))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (authentication == null) {
                                Problems.write(response, json, HttpStatus.UNAUTHORIZED, "unauthenticated", "Entre para continuar.",
                                        request.getRequestURI());
                            } else {
                                response.setStatus(HttpStatus.NO_CONTENT.value());
                            }
                        }))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, e) -> Problems.write(response, json,
                                HttpStatus.UNAUTHORIZED, "unauthenticated", "Entre para continuar.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, e) -> Problems.write(response, json,
                                HttpStatus.FORBIDDEN, "forbidden", "Sem permissão para esta ação.", request.getRequestURI())));
        return http.build();
    }

    /** O firewall do Spring Security recusa endereços como `/api//me` antes do MVC; a recusa também sai em RFC 9457. */
    @Bean
    RequestRejectedHandler requestRejectedHandler(JsonMapper json) {
        return (request, response, e) -> Problems.write(response, json, HttpStatus.BAD_REQUEST, "request_rejected",
                "Endereço de requisição inválido.", request.getRequestURI());
    }

    @Bean
    JdbcOneTimeTokenService oneTimeTokenService(JdbcTemplate jdbc, Clock clock) {
        JdbcOneTimeTokenService service = new JdbcOneTimeTokenService(jdbc);
        service.setClock(clock);
        return service;
    }

    /** Qualquer e-mail pode entrar pelo link; o acesso a um negócio é decidido depois, pela associação. */
    @Bean
    UserDetailsService userDetailsService() {
        return username -> User.withUsername(username)
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    @Bean
    FilterRegistrationBean<SessionLifetimeFilter> sessionLifetimeFilter(Clock clock) {
        var registration = new FilterRegistrationBean<>(new SessionLifetimeFilter(clock));
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER + 1);
        return registration;
    }

    private static GenerateOneTimeTokenRequestResolver linkValidity() {
        DefaultGenerateOneTimeTokenRequestResolver delegate = new DefaultGenerateOneTimeTokenRequestResolver();
        return request -> {
            GenerateOneTimeTokenRequest generated = delegate.resolve(request);
            return generated == null ? null
                    : new GenerateOneTimeTokenRequest(EmailAddresses.normalize(generated.getUsername()), LINK_VALIDITY);
        };
    }

    /** Lê o token CSRF em toda requisição para que o cookie XSRF-TOKEN chegue ao app web antes do primeiro POST. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
