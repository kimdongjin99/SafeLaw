package capstone.safelaw.auth;

import java.util.Set;

// 소셜 로그인 제공자별 ID 토큰 서명 공개키(JWKS) 주소와 발급자(iss)
public enum SocialProvider {

    GOOGLE("https://www.googleapis.com/oauth2/v3/certs",
            Set.of("accounts.google.com", "https://accounts.google.com")),
    APPLE("https://appleid.apple.com/auth/keys",
            Set.of("https://appleid.apple.com"));

    private final String jwksUri;
    private final Set<String> issuers;

    SocialProvider(String jwksUri, Set<String> issuers) {
        this.jwksUri = jwksUri;
        this.issuers = issuers;
    }

    public String jwksUri() {
        return jwksUri;
    }

    public Set<String> issuers() {
        return issuers;
    }
}
