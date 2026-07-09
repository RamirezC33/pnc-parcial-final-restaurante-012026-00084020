package com.uca.pncparcialfinalrestaurante.service;

import com.uca.pncparcialfinalrestaurante.domain.RefreshToken;
import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import com.uca.pncparcialfinalrestaurante.exception.BusinessException;
import com.uca.pncparcialfinalrestaurante.repository.UsuarioRepository;
import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.security.JwtService;
import com.uca.pncparcialfinalrestaurante.web.dto.ChangePasswordRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.LoginRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.RegisterRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de autenticación: login, renovación de tokens, registro de
 * clientes y cambio de contraseña.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Usuario o contraseña incorrectos");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.crear(userDetails.getUsuario());

        return TokenResponse.bearer(accessToken, refreshToken.getToken());
    }

    /** Renueva el Access Token usando un Refresh Token válido (con rotación). */
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken actual = refreshTokenService.validar(refreshTokenValue);
        RefreshToken rotado = refreshTokenService.rotar(actual);

        CustomUserDetails userDetails = new CustomUserDetails(rotado.getUsuario());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return TokenResponse.bearer(accessToken, rotado.getToken());
    }

    /** Registro público: crea siempre un usuario con rol CLIENTE. */
    @Transactional
    public Usuario register(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new BusinessException("El nombre de usuario ya está en uso");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setRole(Role.CLIENTE);
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    /**
     * Cambio de contraseña. Al cambiarla, se revocan TODOS los refresh tokens
     * del usuario (mitiga sesiones robadas; alineado con la idea de la regla A).
     */
    @Transactional
    public void changePassword(Usuario usuario, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
        usuario.setPassword(passwordEncoder.encode(request.newPassword()));
        usuarioRepository.save(usuario);
        refreshTokenService.revocarTodos(usuario);
    }
}
