package capstone.safelaw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// application.properties의 safelaw.auth.social.* 값을 읽어온다. 값이 없으면 빈 목록.
@ConfigurationProperties(prefix = "safelaw.auth.social")
public record SocialLoginProperties(List<String> googleClientIds, List<String> appleClientIds) {

    public SocialLoginProperties {
        googleClientIds = clean(googleClientIds);
        appleClientIds = clean(appleClientIds);
    }

    private static List<String> clean(List<String> ids) {
        return ids == null ? List.of() : ids.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
