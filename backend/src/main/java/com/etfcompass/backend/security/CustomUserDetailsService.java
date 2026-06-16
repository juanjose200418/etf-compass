package com.etfcompass.backend.security;

import com.etfcompass.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final AppUserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user = userRepository.findByEmailIgnoreCase(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return User.withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .disabled(!user.isActive())
        .authorities(user.getRoles().stream().map(role -> "ROLE_" + role.name()).toList().toArray(String[]::new))
        .build();
  }
}
