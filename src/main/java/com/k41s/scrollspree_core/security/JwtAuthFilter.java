package com.k41s.scrollspree_core.security;

import com.k41s.scrollspree_core.enums.LogLevel;
import com.k41s.scrollspree_core.exceptions.JwtExpiredException;
import com.k41s.scrollspree_core.exceptions.JwtMalformedException;
import com.k41s.scrollspree_core.services.LogService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;
    private final LogService logService;

    private static final String HEADER_PREFIX = "Bearer ";

    public JwtAuthFilter(
            JwtTokenProvider tokenProvider,
            CustomUserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver")
            HandlerExceptionResolver resolver,
            LogService logService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
        this.logService = logService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                String tokenType = tokenProvider.getTokenTypeFromJWT(jwt);
                if (!"access".equals(tokenType)) {
                    throw new MalformedJwtException("Provided token is not an access token");
                }

                String username = tokenProvider.getUsernameFromJWT(jwt);
                String rolesString = tokenProvider.getRolesFromJWT(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                List<GrantedAuthority> authorities =
                        AuthorityUtils.commaSeparatedStringToAuthorityList(rolesString);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException ex) {
            logService.log(LogLevel.WARNING, "Jwt Expired: " +  ex.getMessage());

            resolver.resolveException(request, response, null, new JwtExpiredException(ex.getMessage()));
            return;
        } catch (MalformedJwtException ex) {
            logService.log(LogLevel.ERROR, "Invalid JWT Format: " + ex.getMessage());

            resolver.resolveException(request, response, null, new JwtMalformedException(ex.getMessage()));
            return;
        } catch (UsernameNotFoundException ex) {
            logService.log(LogLevel.WARNING, "Token valid, but user deleted: " + ex.getMessage());
        }
        catch (Exception ex) {
            logService.log(LogLevel.ERROR, "Could not set user authentication in security context or generic error: "
                    + ex.getMessage());

            resolver.resolveException(request, response, null, ex);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(HEADER_PREFIX)) {
            return bearerToken.substring(HEADER_PREFIX.length());
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
