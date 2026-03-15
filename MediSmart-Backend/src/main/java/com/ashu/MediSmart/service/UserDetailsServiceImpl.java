package com.ashu.MediSmart.service;

import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Get role name directly from user
        String roleName = user.getRole() != null
                ? user.getRole().trim().toUpperCase(Locale.ROOT)
                : "USER";

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String roleName = user.getRole() != null
                ? user.getRole().trim().toUpperCase(Locale.ROOT)
                : "USER";
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
}
