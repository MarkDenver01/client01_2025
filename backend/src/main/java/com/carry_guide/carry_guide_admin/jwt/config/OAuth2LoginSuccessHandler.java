package com.carry_guide.carry_guide_admin.jwt.config;

import com.carry_guide.carry_guide_admin.jwt.model.domain.UserDomainService;
import com.carry_guide.carry_guide_admin.jwt.model.entity.Role;
import com.carry_guide.carry_guide_admin.jwt.model.entity.User;
import com.carry_guide.carry_guide_admin.jwt.model.state.RoleState;
import com.carry_guide.carry_guide_admin.jwt.repository.RoleRepository;
import com.carry_guide.carry_guide_admin.jwt.util.JwtUtils;
import com.carry_guide.carry_guide_admin.jwt.security.service.CustomizedUserDetails;
import com.carry_guide.carry_guide_admin.jwt.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Autowired
    private final UserDomainService userDomainService;

    @Autowired
    private final JwtUtils jwtUtils;

    @Value("${base.url}")
    private String baseUrl;

    String email;
    String idAttributeKey;
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        if ("google".equals(authToken.getAuthorizedClientRegistrationId())) {
            DefaultOAuth2User principal = (DefaultOAuth2User) authToken.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();
            String email = attributes.getOrDefault("email", "").toString();
            String name = attributes.getOrDefault("name", "").toString();
            this.email = email;
            this.idAttributeKey = "id";

            userDomainService
                    .findByEmail(email)
                    .ifPresentOrElse(user -> {
                        Authentication securityAuth = getAuthentication(user, attributes, authToken);
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    }, () -> {
                        User newUser = new User();
                        Optional<Role> userRole = roleRepository.findRoleByRoleState(RoleState.ROLE_USER); // fetch existing user
                        if (userRole.isPresent()) {
                            newUser.setRole(userRole.get());
                        } else {
                            // handle the case where the role is not found
                            throw new RuntimeException("Role not found");
                        }
                        newUser.setEmail(email);
                        newUser.setUserName(name);
                        newUser.setSignupMethod(authToken.getAuthorizedClientRegistrationId());
                        userService.registerUser(newUser);

                        Authentication securityAuth = getAuthentication(newUser, attributes, authToken);
                        SecurityContextHolder.getContext().setAuthentication(securityAuth);
                    });
        }
        setAlwaysUseDefaultTargetUrl(true);

        // jwt token
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();

        // extract the necessary attributes
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        Set<SimpleGrantedAuthority> authorities = new HashSet<>(
                oauthUser.getAuthorities().stream()
                        .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                        .collect(Collectors.toList()));
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleState().name()));

        // create customized user details instance
        CustomizedUserDetails userDetails = new CustomizedUserDetails(
                null,
                name,
                email,
                null,
                authorities);

        // generate jwt token
        String jwtToken = jwtUtils.generateToken(userDetails);

        // redirect to the base url with the jwt token
        String targetUrl = UriComponentsBuilder.fromUriString(baseUrl + "/oauth2/redirect")
                .queryParam("token", jwtToken)
                .build().toUriString();
        this.setDefaultTargetUrl(targetUrl);
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private Authentication getAuthentication(User newUser, Map<String, Object> attributes, OAuth2AuthenticationToken authToken) {
        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleState().name())),
                attributes,
                idAttributeKey
        );
        return new OAuth2AuthenticationToken(
                oauthUser,
                List.of(new SimpleGrantedAuthority(newUser.getRole().getRoleState().name())),
                authToken.getAuthorizedClientRegistrationId()
        );
    }
}

