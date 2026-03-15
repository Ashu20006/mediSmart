package com.ashu.MediSmart.security;

import com.ashu.MediSmart.service.TokenBlacklistService;
import com.ashu.MediSmart.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsServiceImpl userDetailsService,
                                   TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

        try {
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);

                //  Check if token is blacklisted
                if (tokenBlacklistService.isTokenBlacklisted(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token is blacklisted. Please login again.");
                    return; // Stop filter chain
                }

                try {
                    username = jwtUtil.extractUsername(token);
                } catch (io.jsonwebtoken.JwtException e) {
                    // Invalid token - just continue without authentication
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Log exception and continue without authentication
            filterChain.doFilter(request, response);
        }
    }
}
