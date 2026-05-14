const fs = require('fs');
const file = 'D:/ConnectSphere/ConnectSphere-Microservices/auth-service/auth-service/src/main/java/com/connectsphere/auth/service/impl/AuthServiceImpl.java';
let content = fs.readFileSync(file, 'utf8');

// 1. Add checkAndApplyAutoVerification method
const autoVerifyMethod = `
    private void checkAndApplyAutoVerification(User user) {
        if (user.isVerifiedBadge()) return; // Already verified
        
        boolean hasFullName = user.getFullName() != null && !user.getFullName().trim().isEmpty();
        boolean hasBio = user.getBio() != null && !user.getBio().trim().isEmpty();
        boolean hasPic = user.getProfilePicUrl() != null && !user.getProfilePicUrl().trim().isEmpty();
        boolean isPremium = user.isPremiumMember() && !isPremiumExpired(user);
        
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
                    "Hi " + user.getFullName() + ",\\n\\nBecause you have completed 100% of your profile and are a valued Premium member, we have automatically verified your account!\\n\\nThe blue checkmark badge has been added to your profile.\\n\\nThank you for being an active part of our community!");
            } catch (Exception e) {
                log.warn("Failed to send auto-verification email to {}", user.getEmail());
            }
        }
    }
`;

// Insert the method before toProfileMap (around line 758)
const toProfileMapMarker = 'private Map<String, Object> toProfileMap(User user';
content = content.replace(toProfileMapMarker, autoVerifyMethod + '\n    ' + toProfileMapMarker);

// 2. Call checkAndApplyAutoVerification in updateProfile
const updateProfileCall = 'userRepository.save(user);\n        checkAndApplyAutoVerification(user);';
content = content.replace('userRepository.save(user);', updateProfileCall);

// 3. Call checkAndApplyAutoVerification in uploadProfilePicture
const uploadPicCall = 'userRepository.save(user);\n            checkAndApplyAutoVerification(user);';
content = content.replace('userRepository.save(user);\n            log.info("Profile picture updated for {}", email);', uploadPicCall + '\n            log.info("Profile picture updated for {}", email);');

// 4. ALSO call it in getProfile so users who are ALREADY 100% get it immediately upon viewing their profile
const getProfileCall = 'User user = userRepository.findByEmail(email)\n                .orElseThrow(() -> new ResourceNotFoundException("User not found"));\n        checkAndApplyAutoVerification(user);';
content = content.replace('User user = userRepository.findByEmail(email)\n                .orElseThrow(() -> new ResourceNotFoundException("User not found"));', getProfileCall);

fs.writeFileSync(file, content);
console.log('done');
