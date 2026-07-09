package com.uca.pncparcialfinalrestaurante.security;

import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adaptador entre nuestra entidad de dominio {@link Usuario} y el contrato
 * {@link UserDetails} que exige Spring Security. Mantiene el dominio limpio de
 * dependencias del framework de seguridad.
 */
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    /** Id de la sucursal del usuario (o null). Clave para la regla de negocio B. */
    public Long getSucursalId() {
        return usuario.getSucursal() != null ? usuario.getSucursal().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security espera el prefijo "ROLE_" para usar hasRole(...).
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}
