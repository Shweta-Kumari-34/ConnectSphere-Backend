package com.connectsphere.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The core entity representing a registered user in the ConnectSphere platform.
 * <p>
 * This entity maps to the "users" table and stores all critical user information,
 * including authentication details, profile metadata, verification status, and premium membership flags.
 * </p>
 *
 * <h3>Entity Relationship</h3>
 * <pre class="mermaid">
 * erDiagram
 *     User {
 *         Long userId PK
 *         String username UK
 *         String email UK
 *         String passwordHash
 *         String role
 *         boolean isActive
 *         boolean verifiedBadge
 *         boolean premiumMember
 *     }
 * </pre>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    // Auto-incremented primary key — assigned by MySQL when the row is first saved.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // Both username and email must be unique across all users — enforced at the database level.
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // We never store plain text passwords. This stores the BCrypt hash of the password.
    @Column(nullable = false)
    private String passwordHash;

    // Optional profile display fields — user can fill these in later.
    private String fullName;
    private String bio;
    private String profilePicUrl;

    // Role can be: USER, ADMIN, or MODERATOR — controls what endpoints they can access.
    private String role;

    // How the user signed up — either "LOCAL" (email+password) or "GOOGLE" / "GITHUB" (OAuth2).
    private String provider;

    // If false, the user registered but hasn't verified their email yet. They cannot log in.
    private boolean isActive;

    // When the account was first created — used for verification eligibility (must be 7+ days old).
    private LocalDateTime createdAt;

    // ─── VERIFICATION BADGE fields ───────────────────────────────────────────
    // If true, a blue checkmark appears on their profile. Earned by going through the verification flow.
    private boolean verifiedBadge;
    private LocalDateTime verifiedApprovedAt;       // when admin approved the request
    private LocalDateTime verifiedBadgeActivatedAt; // when the badge was actually activated (after payment)

    // ─── PREMIUM MEMBERSHIP fields ───────────────────────────────────────────
    // Premium users get extra features: themes, profile boost, analytics, priority support.
    private boolean premiumMember;
    private LocalDateTime premiumActivatedAt;   // when premium started
    private LocalDateTime premiumExpiresAt;     // when premium ends (null = doesn't expire)
    private boolean premiumAutoRenew;           // whether to auto-renew at expiry
    private String premiumTheme;                // e.g., "CLASSIC", "DARK", "GOLD"
    private boolean profileBoostEnabled;        // shows their profile higher in search results
    private boolean analyticsEnabled;           // allows viewing post analytics
    private boolean prioritySupportEnabled;     // fast-track support
    private boolean advancedPrivacyEnabled;     // extra privacy controls
    private boolean searchVisibilityBoostEnabled; // appears earlier in user search results
}
