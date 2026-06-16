package com.etfcompass.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {

  @Column(nullable = false, unique = true, length = 180)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 120)
  private String displayName;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 40)
  private Set<Role> roles = new HashSet<>();

  @Column(nullable = false)
  private boolean active = true;

  @Column(length = 255)
  private String passwordResetCodeHash;

  @Column
  private Instant passwordResetCodeExpiresAt;

  public AppUser(String email, String passwordHash, String displayName) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.roles.add(Role.USER);
  }
}
