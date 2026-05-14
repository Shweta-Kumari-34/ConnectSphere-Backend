package com.connectsphere.auth.service.impl;

import static com.connectsphere.auth.validation.ValidationPatterns.USERNAME;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.auth.dto.AuthResponseDto;
import com.connectsphere.auth.dto.ChangePasswordRequestDto;
import com.connectsphere.auth.dto.InternalSubscriptionActivationRequestDto;
import com.connectsphere.auth.dto.LoginRequestDto;
import com.connectsphere.auth.dto.RegisterRequestDto;
import com.connectsphere.auth.dto.UpdateProfileRequestDto;
import com.connectsphere.auth.dto.VerificationApplyRequestDto;
import com.connectsphere.auth.dto.VerificationReviewRequestDto;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.entity.VerificationRequest;
import com.connectsphere.auth.entity.VerificationRequest.VerificationStatus;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ConflictException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.repository.VerificationRequestRepository;
import com.connectsphere.auth.service.AuthService;
import com.connectsphere.auth.service.EmailService;
import com.connectsphere.auth.service.OtpService;
import com.connectsphere.auth.util.AuthConstants;
import com.connectsphere.auth.util.JwtUtil;

/**
 * <h1>AuthServiceImpl</h1>
 * <p>The primary implementation of {@link AuthService}, orchestrating the core security and identity 
 * logic for the ConnectSphere platform.</p>
 * 
 * <h2>Key Logic Flows:</h2>
 * <h3>1. Registration Workflow</h3>
 * <pre>
 * sequenceDiagram
 *     User->>AuthService: register(details)
 *     AuthService->>DB: Save User (active=false)
 *     AuthService->>OtpService: sendOtp(SIGNUP)
 *     User->>AuthService: verifyRegister(otp)
 *     AuthService->>OtpService: verifyOtp()
 *     AuthService->>DB: Save User (active=true)
 *     AuthService-->>User: Issue JWT
 * </pre>
 * 
 * <h3>2. Multi-Factor Authentication</h3>
 * <p>Supports both traditional password login and OTP-based secure login to prevent unauthorized access.</p>
 * 
 * <h2>Technical Features:</h2>
 * <ul>
 *     <li><b>Security:</b> Password hashing via BCrypt and JWT-based stateless authentication.</li>
 *     <li><b>Caching:</b> Intensive use of Spring Cache for profiles and search results to minimize DB load.</li>
 *     <li><b>Media Handling:</b> Secure file storage for profile pictures and verification documents with MIME-type validation.</li>
 *     <li><b>Verification:</b> Multi-stage verification pipeline (Submitted -> Under Review -> Payment Pending -> Badge Activated).</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    // Constants for file validation
    private static final long MAX_PROFILE_PICTURE_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_VERIFICATION_FILE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_PROFILE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VERIFICATION_DOC_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final Set<String> ALLOWED_SELFIE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME);
    
    // Business Logic Constants removed and replaced by AuthConstants
    
    private final UserRepository userRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${connectsphere.profile-picture.dir:./uploads/profile-pictures}")
    private String profilePictureDir;

    @Value("${connectsphere.verification.dir:./uploads/verification}")
    private String verificationDir;

    @Value("${app.public-auth-url:http://localhost:8090/auth}")
    private String publicAuthUrl;

    @Value("${app.internal-secret:connectsphere-internal-secret}")
    private String internalSecretKey;

    private final OtpService otpService;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           VerificationRequestRepository verificationRequestRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           OtpService otpService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * Registers a new user account.
     * Validates email/username uniqueness and password strength.
     * Account remains inactive until email is verified via OTP.
     * 
     * @param request Registration details (email, username, password, etc.)
     * @return AuthResponseDto with instructions for verification
     * @throws ConflictException if email or username already exists
     * @throws BadRequestException if password is too short
     */
    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        if (request.getEmail() == null || request.getUsername() == null || request.getPassword() == null || request.getFullName() == null) {
            throw new BadRequestException("Email, username, password, and full name are required");
        }
        
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();
        String fullName = request.getFullName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists");
        }
        if (request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(fullName);
        
        String role = request.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = AuthConstants.ROLE_USER;
        }
        user.setRole(role.toUpperCase());
        
        user.setProvider(AuthConstants.PROVIDER_LOCAL);
        user.setActive(false); // require OTP verification
        user.setCreatedAt(LocalDateTime.now());
        user.setPremiumTheme(AuthConstants.THEME_CLASSIC);
        user.setPremiumAutoRenew(true);

        User savedUser = userRepository.save(user);
        checkAndApplyAutoVerification(user);
        
        // Trigger OTP generation for email verification
        otpService.generateAndSendOtp(savedUser.getEmail(), AuthConstants.OTP_PURPOSE_SIGNUP);

        log.info("User registration initiated: {}", savedUser.getEmail());

        return new AuthResponseDto("OTP sent to email. Please verify to activate account.", false);
    }

    /**
     * Verifies the OTP sent during registration to activate the account.
     * 
     * @param email The registered email
     * @param otp The 6-digit OTP code
     * @return AuthResponseDto with the JWT token and user info
     * @throws ResourceNotFoundException if user doesn't exist
     * @throws BadRequestException if user is already active or OTP is invalid
     */
    @Override
    public AuthResponseDto verifyRegister(String email, String otp) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isActive()) {
            throw new BadRequestException("User is already active");
        }

        // Verify OTP from cache/database
        otpService.verifyOtp(user.getEmail(), otp, AuthConstants.OTP_PURPOSE_SIGNUP);

        user.setActive(true);
        userRepository.save(user);

        // Generate initial JWT token for the session
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        
        emailService.sendEmail(user.getEmail(),
                "Welcome to ConnectSphere! 🎉",
                "Hi " + user.getFullName() + ",\n\nYour email has been verified and your ConnectSphere account is now active.\n\nStart exploring — share posts, follow friends, and discover trending content.\n\nEnjoy the platform!");

        return new AuthResponseDto(
                "Account verified and activated successfully",
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public AuthResponseDto initiateLogin(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is not active");
        }

        otpService.generateAndSendOtp(user.getEmail(), AuthConstants.OTP_PURPOSE_LOGIN);

        return new AuthResponseDto("OTP sent to email for login", false);
    }

    @Override
    public AuthResponseDto verifyLogin(String email, String otp) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is not active");
        }

        otpService.verifyOtp(user.getEmail(), otp, AuthConstants.OTP_PURPOSE_LOGIN);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        
        emailService.sendEmail(user.getEmail(),
                "New Login Detected",
                "Hi " + user.getFullName() + ",\n\nA new login to your ConnectSphere account was just detected using an OTP code.\n\nTime: " + LocalDateTime.now() + "\n\nIf this was you, no action is needed. If you didn't sign in, please reset your password immediately.");

        return new AuthResponseDto(
                "Login successful",
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.generateAndSendOtp(user.getEmail(), AuthConstants.OTP_PURPOSE_RESET);
        return "OTP sent to your email for password reset";
    }

    @Override
    public String resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters long");
        }

        otpService.verifyOtp(user.getEmail(), otp, AuthConstants.OTP_PURPOSE_RESET);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendEmail(user.getEmail(),
                "Password Changed Successfully",
                "Hi " + user.getFullName() + ",\n\nYour ConnectSphere account password was successfully changed.\n\nTime: " + java.time.LocalDateTime.now() + "\n\nIf you didn't make this change, contact support immediately and reset your password.");

        return "Password reset successfully";
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadRequestException("Email and password are required");
        }
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("User logged in successfully: {}", user.getEmail());
        
        emailService.sendEmail(user.getEmail(),
                "New Login Detected",
                "Hi " + user.getFullName() + ",\n\nA new password-based login to your ConnectSphere account was just detected.\n\nTime: " + java.time.LocalDateTime.now() + "\n\nIf this was you, no action is needed. If you didn't sign in, reset your password immediately.");

        return new AuthResponseDto(
                "Login successful",
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    public String changePassword(String email, ChangePasswordRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        checkAndApplyAutoVerification(user);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for {}", email);
        return "Password changed successfully";
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByEmail", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'"),
            @CacheEvict(value = "authVerificationEligibility", key = "#a0")
    })
    public String updateProfile(String email, UpdateProfileRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (request.getProfilePicUrl() != null) {
            user.setProfilePicUrl(request.getProfilePicUrl().trim());
        }

        userRepository.save(user);
        checkAndApplyAutoVerification(user);
        log.info("Profile updated for {}", email);
        return "Profile updated successfully";
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByEmail", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'"),
            @CacheEvict(value = "authVerificationEligibility", key = "#a0")
    })
    public String uploadProfilePicture(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, and WebP profile pictures are allowed");
        }

        if (file.getSize() > MAX_PROFILE_PICTURE_BYTES) {
            throw new BadRequestException("Profile picture must be 5MB or smaller");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            Path uploadDir = Paths.get(profilePictureDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String extension = getExtension(file.getOriginalFilename(), contentType);
            String filename = "profile_" + UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(filename).normalize();
            file.transferTo(targetPath);

            String profilePicUrl = publicAuthUrl + "/profile-picture/" + filename;
            user.setProfilePicUrl(profilePicUrl);
            userRepository.save(user);
            checkAndApplyAutoVerification(user);
            log.info("Profile picture updated for {}", email);
            return profilePicUrl;
        } catch (Exception ex) {
            log.error("Failed to store profile picture for {}", email, ex);
            throw new BadRequestException("Failed to upload profile picture");
        }
    }

    @Override
    public Resource loadProfilePicture(String filename) {
        try {
            Path uploadDir = Paths.get(profilePictureDir).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(Paths.get(filename).getFileName().toString()).normalize();
            if (!filePath.startsWith(uploadDir) || !Files.exists(filePath)) {
                throw new ResourceNotFoundException("Profile picture not found");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Profile picture not found");
            }
            return resource;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to load profile picture {}", filename, ex);
            throw new ResourceNotFoundException("Profile picture not found");
        }
    }

    @Override
    public Resource loadVerificationFile(String filename) {
        try {
            Path uploadDir = Paths.get(verificationDir).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(Paths.get(filename).getFileName().toString()).normalize();
            if (!filePath.startsWith(uploadDir) || !Files.exists(filePath)) {
                throw new ResourceNotFoundException("Verification file not found");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Verification file not found");
            }
            return resource;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to load verification file {}", filename, ex);
            throw new ResourceNotFoundException("Verification file not found");
        }
    }

    @Override
    @Cacheable(value = "authProfileByEmail", key = "#a0", unless = "#result == null")
    public Map<String, Object> getProfile(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toProfileMap(user, includeVerificationSummary(user.getEmail()));
    }

    @Override
    @Cacheable(value = "authProfileByUserId", key = "#a0", unless = "#result == null")
    public Map<String, Object> getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return toProfileMap(user, includeVerificationSummary(user.getEmail()));
    }

    @Override
    @Cacheable(value = "authUserSearch", key = "#a0 == null ? '' : #a0.trim().toLowerCase()", unless = "#result == null")
    public List<Map<String, Object>> searchUsers(String keyword) {
        String query = keyword == null ? "" : keyword.trim();
        List<User> users = new ArrayList<>(userRepository.findByUsernameContainingIgnoreCase(query));
        List<User> nameMatches = userRepository.findByFullNameContainingIgnoreCase(query);

        for (User user : nameMatches) {
            if (users.stream().noneMatch(existing -> existing.getEmail().equals(user.getEmail()))) {
                users.add(user);
            }
        }

        return users.stream()
                .filter(User::isActive)
                .sorted(Comparator
                        .comparing(User::isPremiumMember).reversed()
                        .thenComparing(User::isVerifiedBadge).reversed()
                        .thenComparing((User u) -> !u.getUsername().equalsIgnoreCase(query))
                        .thenComparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", user.getUserId());
                    map.put("username", user.getUsername());
                    map.put("email", user.getEmail());
                    map.put("fullName", user.getFullName());
                    map.put("profilePicUrl", user.getProfilePicUrl());
                    map.put("bio", user.getBio());
                    map.put("role", user.getRole());
                    map.put("isVerified", user.isVerifiedBadge());
                    map.put("isPremiumMember", isPremiumActive(user));
                    map.put("searchBoost", user.isSearchVisibilityBoostEnabled());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByEmail", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'")
    })
    public String deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
        log.info("Account deactivated for {}", email);
        return "Account deactivated successfully";
    }

    @Override
    public AuthResponseDto refreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDto(
                "Token refreshed successfully",
                newToken,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Override
    @Cacheable(value = "authAllUsers", key = "'all'", unless = "#result == null")
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getUserId());
                    map.put("username", user.getUsername());
                    map.put("email", user.getEmail());
                    map.put("fullName", user.getFullName());
                    map.put("role", user.getRole());
                    map.put("active", user.isActive());
                    map.put("createdAt", user.getCreatedAt());
                    map.put("isVerified", user.isVerifiedBadge());
                    map.put("isPremiumMember", isPremiumActive(user));
                    map.put("premiumExpiresAt", user.getPremiumExpiresAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByUserId", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'")
    })
    public String suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User suspended: {}", user.getEmail());
        return "User " + user.getUsername() + " has been suspended";
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByUserId", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'")
    })
    public String reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setActive(true);
        userRepository.save(user);
        log.info("User reactivated: {}", user.getEmail());
        return "User " + user.getUsername() + " has been reactivated";
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByUserId", key = "#a0"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'")
    })
    public String deleteUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        userRepository.delete(user);
        log.info("User deleted permanently: {}", user.getEmail());
        return "User " + user.getUsername() + " has been permanently deleted";
    }

    @Override
    public Map<String, Object> validateVerificationUsername(String username) {
        String candidate = username == null ? "" : username.trim();
        Map<String, Object> response = new HashMap<>();
        boolean valid = USERNAME_PATTERN.matcher(candidate).matches();
        response.put("valid", valid);
        response.put("username", candidate);
        response.put("message", valid
                ? "Username is eligible for verification"
                : "Username must be 3-20 characters and use letters, numbers, dots, or underscores");
        return response;
    }

    @Override
    @Cacheable(value = "authVerificationEligibility", key = "#a0", unless = "#result == null")
    public Map<String, Object> getVerificationEligibility(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> blockers = new ArrayList<>();
        if (!user.isActive()) {
            blockers.add("Account must be active");
        }
        if (!USERNAME_PATTERN.matcher(user.getUsername() == null ? "" : user.getUsername()).matches()) {
            blockers.add("Username is not eligible");
        }
        if (user.getProfilePicUrl() == null || user.getProfilePicUrl().isBlank()) {
            blockers.add("Profile photo is required");
        }
        if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            blockers.add("Account must be at least 7 days old");
        }
        if (user.isVerifiedBadge()) {
            blockers.add("Account is already verified");
        }

        VerificationRequest latest = getLatestVerificationRequest(user.getEmail()).orElse(null);
        if (latest != null && latest.getStatus() != VerificationStatus.REJECTED && latest.getStatus() != VerificationStatus.BADGE_ACTIVATED) {
            blockers.add("You already have an active verification request");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("eligible", blockers.isEmpty());
        response.put("blockers", blockers);
        response.put("username", user.getUsername());
        response.put("profilePicPresent", user.getProfilePicUrl() != null && !user.getProfilePicUrl().isBlank());
        response.put("accountCreatedAt", user.getCreatedAt());
        response.put("existingRequest", latest == null ? null : toVerificationMap(latest));
        return response;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authVerificationEligibility", key = "#a0"),
            @CacheEvict(value = "authVerificationMine", key = "#a0"),
            @CacheEvict(value = "authProfileByEmail", key = "#a0")
    })
    public Map<String, Object> submitVerificationRequest(String email,
                                                         VerificationApplyRequestDto request,
                                                         MultipartFile document,
                                                         MultipartFile selfie) {
        Map<String, Object> eligibility = getVerificationEligibility(email);
        if (!Boolean.TRUE.equals(eligibility.get("eligible"))) {
            throw new BadRequestException("Account is not eligible for verification yet");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.isIncludeDocument() && (document == null || document.isEmpty())) {
            throw new BadRequestException("Document upload is required when includeDocument is enabled");
        }
        if (request.isIncludeSelfie() && (selfie == null || selfie.isEmpty())) {
            throw new BadRequestException("Selfie upload is required when includeSelfie is enabled");
        }

        String documentUrl = storeVerificationAsset(email, "document", document, ALLOWED_VERIFICATION_DOC_TYPES);
        String selfieUrl = storeVerificationAsset(email, "selfie", selfie, ALLOWED_SELFIE_TYPES);

        VerificationRequest verificationRequest = new VerificationRequest();
        verificationRequest.setUserEmail(email);
        verificationRequest.setUsernameSnapshot(user.getUsername());
        verificationRequest.setFullNameSnapshot(user.getFullName());
        verificationRequest.setProfilePicUrlSnapshot(user.getProfilePicUrl());
        verificationRequest.setReason(trimToNull(request.getReason()));
        verificationRequest.setDocumentUrl(documentUrl);
        verificationRequest.setSelfieUrl(selfieUrl);
        verificationRequest.setStatus(VerificationStatus.SUBMITTED);
        verificationRequest.setSubmittedAt(LocalDateTime.now());
        verificationRequestRepository.save(verificationRequest);

        Map<String, Object> response = toVerificationMap(verificationRequest);
        response.put("message", "Verification request submitted. Our team will review it soon.");
        return response;
    }

    @Override
    @Cacheable(value = "authVerificationMine", key = "#a0", unless = "#result == null")
    public Map<String, Object> getMyVerificationRequest(String email) {
        return getLatestVerificationRequest(email)
                .map(this::toVerificationMap)
                .orElseGet(() -> {
                    Map<String, Object> empty = new HashMap<>();
                    empty.put("status", "NONE");
                    empty.put("message", "No verification request submitted yet");
                    return empty;
                });
    }

    @Override
    public List<Map<String, Object>> getVerificationRequestsForAdmin(String statusFilter) {
        List<VerificationRequest> requests;
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            requests = verificationRequestRepository.findAll().stream()
                    .sorted(Comparator.comparing(VerificationRequest::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        } else if ("PENDING".equalsIgnoreCase(statusFilter)) {
            requests = verificationRequestRepository.findAllByStatusInOrderBySubmittedAtAsc(List.of(
                    VerificationStatus.SUBMITTED, VerificationStatus.UNDER_REVIEW, VerificationStatus.PAYMENT_PENDING));
        } else {
            VerificationStatus status;
            try {
                status = VerificationStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid status filter");
            }
            requests = verificationRequestRepository.findAll().stream()
                    .filter(item -> item.getStatus() == status)
                    .sorted(Comparator.comparing(VerificationRequest::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        return requests.stream().map(this::toVerificationMap).collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authVerificationMine", allEntries = true),
            @CacheEvict(value = "authVerificationEligibility", allEntries = true),
            @CacheEvict(value = "authProfileByEmail", allEntries = true)
    })
    public Map<String, Object> reviewVerificationRequest(Long requestId, String adminEmail, VerificationReviewRequestDto request) {
        VerificationRequest verificationRequest = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        String decision = request.getDecision().trim().toUpperCase();
        verificationRequest.setReviewedBy(trimToNull(adminEmail));
        verificationRequest.setAdminNote(trimToNull(request.getAdminNote()));
        verificationRequest.setReviewedAt(LocalDateTime.now());

        if ("APPROVE".equals(decision)) {
            verificationRequest.setStatus(VerificationStatus.PAYMENT_PENDING);
            verificationRequest.setRejectionReason(null);
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required when rejecting a request");
            }
            verificationRequest.setStatus(VerificationStatus.REJECTED);
            verificationRequest.setRejectionReason(request.getRejectionReason().trim());
        }

        verificationRequestRepository.save(verificationRequest);
        Map<String, Object> response = toVerificationMap(verificationRequest);
        response.put("message", "Verification request reviewed successfully");
        return response;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "authProfileByEmail", key = "#a1.userEmail"),
            @CacheEvict(value = "authVerificationMine", key = "#a1.userEmail"),
            @CacheEvict(value = "authVerificationEligibility", key = "#a1.userEmail"),
            @CacheEvict(value = "authUserSearch", allEntries = true),
            @CacheEvict(value = "authAllUsers", key = "'all'")
    })
    public Map<String, Object> applyInternalSubscriptionActivation(String internalSecret, InternalSubscriptionActivationRequestDto request) {
        if (internalSecret == null || !internalSecret.equals(internalSecretKey)) {
            throw new BadRequestException("Invalid internal integration secret");
        }

        User user = userRepository.findByEmail(request.getUserEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String planCode = request.getPlanCode().trim().toUpperCase();
        if (AuthConstants.PLAN_VERIFIED_BADGE.equals(planCode)) {
            user.setVerifiedBadge(request.isActive());
            if (request.isActive()) {
                user.setVerifiedApprovedAt(LocalDateTime.now());
                user.setVerifiedBadgeActivatedAt(LocalDateTime.now());
                getLatestVerificationRequest(user.getEmail()).ifPresent(existing -> {
                    existing.setStatus(VerificationStatus.BADGE_ACTIVATED);
                    existing.setReviewedAt(LocalDateTime.now());
                    verificationRequestRepository.save(existing);
                });
            }
        } else if (AuthConstants.PLAN_PREMIUM_MEMBERSHIP.equals(planCode)) {
            user.setPremiumMember(request.isActive());
            user.setPremiumAutoRenew(request.isAutoRenew());
            user.setPremiumTheme(request.getTheme() == null || request.getTheme().isBlank() ? AuthConstants.THEME_CLASSIC : request.getTheme().trim().toUpperCase());
            user.setPremiumActivatedAt(request.isActive() ? LocalDateTime.now() : user.getPremiumActivatedAt());
            user.setPremiumExpiresAt(request.getExpiresAt());
            user.setProfileBoostEnabled(request.isActive());
            user.setAnalyticsEnabled(request.isActive());
            user.setPrioritySupportEnabled(request.isActive());
            user.setAdvancedPrivacyEnabled(request.isActive());
            user.setSearchVisibilityBoostEnabled(request.isActive());
        } else {
            throw new BadRequestException("Unsupported plan code");
        }

        userRepository.save(user);
        log.info("Internal subscription update applied for {} with plan {}", user.getEmail(), planCode);

        if (request.isActive()) {
            if (AuthConstants.PLAN_VERIFIED_BADGE.equals(planCode)) {
                emailService.sendEmail(user.getEmail(),
                    "ConnectSphere Verified Badge Activated",
                    "Hi " + user.getFullName() + ",\n\nYour verified badge is now active on ConnectSphere.\n\n" +
                    "Your profile and content will now display the verified badge across the platform.\n\n" +
                    "Reference ID: " + (request.getSourcePaymentReference() != null ? request.getSourcePaymentReference() : "N/A") + "\n\n" +
                    "Thank you for completing the verification process.");
            } else {
                emailService.sendEmail(user.getEmail(),
                    "ConnectSphere Premium Activated! 💎",
                    "Hi " + user.getFullName() + ",\n\nCongratulations! Your premium membership is now active.\n\n" +
                    "You now have access to exclusive features:\n" +
                    "- Premium Themes\n- Profile Boost\n- Advanced Analytics\n- Priority Support\n\n" +
                    "Reference ID: " + (request.getSourcePaymentReference() != null ? request.getSourcePaymentReference() : "N/A") + "\n\n" +
                    "Thank you for being a premium member of ConnectSphere!");
            }
        }

        Map<String, Object> response = toProfileMap(user, includeVerificationSummary(user.getEmail()));
        response.put("planCode", planCode);
        response.put("sourcePaymentReference", request.getSourcePaymentReference());
        return response;
    }

    
    private void checkAndApplyAutoVerification(User user) {
        if (user.isVerifiedBadge()) return; // Already verified
        
        boolean hasFullName = user.getFullName() != null && !user.getFullName().trim().isEmpty();
        boolean hasBio = user.getBio() != null && !user.getBio().trim().isEmpty();
        boolean hasPic = user.getProfilePicUrl() != null && !user.getProfilePicUrl().trim().isEmpty();
        boolean isPremium = isPremiumActive(user);
        
        // Auto-verify if 100% complete and Premium
        if (hasFullName && hasBio && hasPic && isPremium) {
            log.info("Auto-verifying user {} due to 100% profile completion and Premium status", user.getEmail());
            user.setVerifiedBadge(true);
            user.setVerifiedApprovedAt(LocalDateTime.now());
            user.setVerifiedBadgeActivatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            try {
                emailService.sendEmail(user.getEmail(),
                    "Congratulations! You are now Verified on ConnectSphere! ✅",
                    "Hi " + user.getFullName() + ",\n\nBecause you have completed 100% of your profile and are a valued Premium member, we have automatically verified your account!\n\nThe blue checkmark badge has been added to your profile.\n\nThank you for being an active part of our community!");
            } catch (Exception e) {
                log.warn("Failed to send auto-verification email to {}", user.getEmail());
            }
        }
    }

    private Map<String, Object> toProfileMap(User user, Map<String, Object> verificationSummary) {
        if (isPremiumExpired(user)) {
            user.setPremiumMember(false);
            user.setProfileBoostEnabled(false);
            user.setAnalyticsEnabled(false);
            user.setPrioritySupportEnabled(false);
            user.setAdvancedPrivacyEnabled(false);
            user.setSearchVisibilityBoostEnabled(false);
            try {
                userRepository.save(user);
            } catch (Exception ex) {
                log.warn("Unable to persist premium-expired flags for {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("bio", user.getBio());
        profile.put("profilePicUrl", user.getProfilePicUrl());
        profile.put("role", user.getRole());
        profile.put("isActive", user.isActive());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("isVerified", user.isVerifiedBadge());
        profile.put("verifiedApprovedAt", user.getVerifiedApprovedAt());
        profile.put("verifiedBadgeActivatedAt", user.getVerifiedBadgeActivatedAt());
        profile.put("isPremiumMember", user.isPremiumMember());
        profile.put("premiumActivatedAt", user.getPremiumActivatedAt());
        profile.put("premiumExpiresAt", user.getPremiumExpiresAt());
        profile.put("premiumAutoRenew", user.isPremiumAutoRenew());
        profile.put("premiumTheme", user.getPremiumTheme());
        profile.put("profileBoostEnabled", user.isProfileBoostEnabled());
        profile.put("analyticsEnabled", user.isAnalyticsEnabled());
        profile.put("prioritySupportEnabled", user.isPrioritySupportEnabled());
        profile.put("advancedPrivacyEnabled", user.isAdvancedPrivacyEnabled());
        profile.put("searchVisibilityBoostEnabled", user.isSearchVisibilityBoostEnabled());
        profile.put("subscriptionStatus", user.isPremiumMember() ? "ACTIVE" : "EXPIRED");
        profile.put("verification", verificationSummary);
        return profile;
    }

    private Map<String, Object> includeVerificationSummary(String email) {
        VerificationRequest latest = getLatestVerificationRequest(email).orElse(null);
        if (latest == null) {
            return Map.of("status", "NONE");
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", latest.getId());
        summary.put("status", latest.getStatus().name());
        summary.put("submittedAt", latest.getSubmittedAt());
        summary.put("reviewedAt", latest.getReviewedAt());
        summary.put("rejectionReason", latest.getRejectionReason());
        return summary;
    }

    private Optional<VerificationRequest> getLatestVerificationRequest(String email) {
        return verificationRequestRepository.findFirstByUserEmailOrderBySubmittedAtDesc(email);
    }
    
    private boolean isPremiumActive(User user) {
        return user.isPremiumMember() && !isPremiumExpired(user);
    }

    private String storeVerificationAsset(String email, String kind, MultipartFile file, Set<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_VERIFICATION_FILE_BYTES) {
            throw new BadRequestException("Verification file must be 10MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("Unsupported verification file format");
        }

        try {
            Path uploadDir = Paths.get(verificationDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String extension = getExtension(file.getOriginalFilename(), contentType);
            String filename = kind + "_" + email.replace("@", "_at_") + "_" + UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(filename).normalize();
            file.transferTo(targetPath);
            return publicAuthUrl + "/verification-file/" + filename;
        } catch (Exception ex) {
            log.error("Failed to store verification file for {}", email, ex);
            throw new BadRequestException("Failed to upload verification file");
        }
    }

    private Map<String, Object> toVerificationMap(VerificationRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("userEmail", request.getUserEmail());
        map.put("username", request.getUsernameSnapshot());
        map.put("fullName", request.getFullNameSnapshot());
        map.put("profilePicUrl", request.getProfilePicUrlSnapshot());
        map.put("reason", request.getReason());
        map.put("documentUrl", request.getDocumentUrl());
        map.put("selfieUrl", request.getSelfieUrl());
        map.put("status", request.getStatus().name());
        map.put("rejectionReason", request.getRejectionReason());
        map.put("adminNote", request.getAdminNote());
        map.put("reviewedBy", request.getReviewedBy());
        map.put("reviewedAt", request.getReviewedAt());
        map.put("submittedAt", request.getSubmittedAt());
        return map;
    }

    private boolean isPremiumExpired(User user) {
        return user.isPremiumMember()
                && user.getPremiumExpiresAt() != null
                && user.getPremiumExpiresAt().isBefore(LocalDateTime.now());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                return originalFilename.substring(dotIndex);
            }
        }

        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        if ("application/pdf".equals(contentType)) {
            return ".pdf";
        }
        return ".jpg";
    }
}
