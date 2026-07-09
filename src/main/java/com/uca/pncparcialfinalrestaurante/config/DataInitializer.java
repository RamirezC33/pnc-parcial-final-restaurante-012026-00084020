package com.uca.pncparcialfinalrestaurante.config;

import com.uca.pncparcialfinalrestaurante.domain.EstadoMesa;
import com.uca.pncparcialfinalrestaurante.domain.Mesa;
import com.uca.pncparcialfinalrestaurante.domain.Producto;
import com.uca.pncparcialfinalrestaurante.domain.Role;
import com.uca.pncparcialfinalrestaurante.domain.Sucursal;
import com.uca.pncparcialfinalrestaurante.domain.Usuario;
import com.uca.pncparcialfinalrestaurante.repository.MesaRepository;
import com.uca.pncparcialfinalrestaurante.repository.ProductoRepository;
import com.uca.pncparcialfinalrestaurante.repository.SucursalRepository;
import com.uca.pncparcialfinalrestaurante.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga datos iniciales SOLO si la base está vacía: dos sucursales, un usuario de
 * cada rol y algunos productos. Sirve para probar la API y la regla de negocio B
 * de inmediato tras {@code docker-compose up}.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final SucursalRepository sucursalRepository;
    private final MesaRepository mesaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username}")
    private String adminUsername;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    public DataInitializer(SucursalRepository sucursalRepository, MesaRepository mesaRepository,
                           ProductoRepository productoRepository, UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.sucursalRepository = sucursalRepository;
        this.mesaRepository = mesaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return; // Ya inicializado: no duplicar datos.
        }

        Sucursal centro = crearSucursal("Sucursal Centro", "Av. Principal 100");
        Sucursal norte = crearSucursal("Sucursal Norte", "Blvd. Norte 250");

        crearMesa(1, 4, centro);
        crearMesa(2, 2, centro);
        crearMesa(1, 6, norte);

        // Un usuario de cada rol. El encargado pertenece a la Sucursal Centro:
        // solo podrá gestionar mesas y pedidos de esa sucursal (regla B).
        crearUsuario(adminUsername, adminPassword, "Administrador General", Role.ADMINISTRADOR, null);
        crearUsuario("encargado.centro", "Encargado123!", "Encargado Centro", Role.ENCARGADO, centro);
        crearUsuario("encargado.norte", "Encargado123!", "Encargado Norte", Role.ENCARGADO, norte);
        crearUsuario("cliente", "Cliente123!", "Cliente Demo", Role.CLIENTE, null);

        crearProducto("Hamburguesa Clásica", "Carne de res, queso y vegetales", "8.50");
        crearProducto("Pizza Margarita", "Salsa de tomate, mozzarella y albahaca", "12.00");
        crearProducto("Ensalada César", "Lechuga, crotones, aderezo césar", "6.75");
        crearProducto("Refresco", "Bebida gaseosa 355ml", "1.75");
    }

    private Sucursal crearSucursal(String nombre, String direccion) {
        Sucursal s = new Sucursal();
        s.setNombre(nombre);
        s.setDireccion(direccion);
        return sucursalRepository.save(s);
    }

    private void crearMesa(int numero, int capacidad, Sucursal sucursal) {
        Mesa m = new Mesa();
        m.setNumero(numero);
        m.setCapacidad(capacidad);
        m.setEstado(EstadoMesa.LIBRE);
        m.setSucursal(sucursal);
        mesaRepository.save(m);
    }

    private void crearUsuario(String username, String password, String nombre, Role role, Sucursal sucursal) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setNombre(nombre);
        u.setRole(role);
        u.setSucursal(sucursal);
        u.setActivo(true);
        usuarioRepository.save(u);
    }

    private void crearProducto(String nombre, String descripcion, String precio) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setDisponible(true);
        productoRepository.save(p);
    }
}
