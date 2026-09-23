package com.k41s.scrollspree_core.services;

import com.k41s.scrollspree_core.dtos.AuthenticatedUserDTO;
import com.k41s.scrollspree_core.dtos.LoginDTO;
import com.k41s.scrollspree_core.dtos.RegisterUserDTO;
import com.k41s.scrollspree_core.entities.User;
import com.k41s.scrollspree_core.entities.Cart;
import com.k41s.scrollspree_core.enums.Role;
import com.k41s.scrollspree_core.exceptions.JwtMalformedException;
import com.k41s.scrollspree_core.exceptions.UserValidationException;
import com.k41s.scrollspree_core.mappers.UserMapper;
import com.k41s.scrollspree_core.repositories.UserRepository;
import com.k41s.scrollspree_core.security.CustomUserDetails;
import com.k41s.scrollspree_core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper mapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticatedUserDTO login(LoginDTO loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return generateTokenAndMapDTO(authentication);
    }

    @Transactional
    public AuthenticatedUserDTO registerAndLogin(RegisterUserDTO dto) {
        validateRegistrationCredentials(dto);

        User user = mapper.fromRegisterDto(dto);
        String rawPassword = dto.getPassword();
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.USER);

        Cart cart = new Cart();
        cart.setUser(user);
        user.setCart(cart);

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return generateTokenAndMapDTO(authentication);
    }

    public AuthenticatedUserDTO refreshAccessToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new JwtMalformedException("Invalid refresh token");
        }

        String tokenType = tokenProvider.getTokenTypeFromJWT(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new JwtMalformedException("Provided token is not a refresh token");
        }

        String username = tokenProvider.getUsernameFromJWT(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserValidationException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        return generateTokenAndMapDTO(authentication);
    }

    private AuthenticatedUserDTO generateTokenAndMapDTO(Authentication authentication) {
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.user();
        AuthenticatedUserDTO authUserDto = mapper.toAuthenticatedUserDto(user);
        authUserDto.setToken(accessToken);
        authUserDto.setRefreshToken(refreshToken);

        return authUserDto;
    }

    private void validateRegistrationCredentials(RegisterUserDTO dto) {
        if (
                userRepository.existsByUsername(dto.getUsername())
                        || userRepository.existsByEmail(dto.getEmail())
        ) {
            throw new UserValidationException("Username or email already exists");
        }

    }
}
