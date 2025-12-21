package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.UsuarioRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;            // null si mongo
    private final UsuarioMongoRepository usuarioMongoRepository;  // null si sql
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final String dbEngine;

    public UsuarioService(
            ObjectProvider<UsuarioRepository> usuarioRepository,
            ObjectProvider<UsuarioMongoRepository> usuarioMongoRepository,
            ObjectProvider<PasswordEncoder> passwordEncoderProvider,
            @Value("${app.db.engine:h2}") String dbEngine
    ) {
        this.usuarioRepository = usuarioRepository.getIfAvailable();
        this.usuarioMongoRepository = usuarioMongoRepository.getIfAvailable();
        this.passwordEncoderProvider = passwordEncoderProvider;
        this.dbEngine = (dbEngine == null ? "h2" : dbEngine.toLowerCase());
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        if (isMongo()) {
            if (usuarioMongoRepository == null) {
                throw new IllegalStateException("UsuarioMongoRepository no disponible (perfil mongo mal configurado)");
            }

            UsuarioMongo u = usuarioMongoRepository.findByCorreo(correo)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en Mongo: " + correo));

            return User.withUsername(u.getCorreo())
                    .password(u.getPassword())
                    .roles("USER")
                    .build();
        }

        if (usuarioRepository == null) {
            throw new IllegalStateException("UsuarioRepository no disponible (perfil sql mal configurado)");
        }

        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en SQL: " + correo));

        return User.withUsername(u.getCorreo())
                .password(u.getPassword())
                .roles("USER")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean existePorCorreo(String correo) {
        if (isMongo()) {
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");
            return usuarioMongoRepository.existsByCorreo(correo);
        }
        if (usuarioRepository == null) throw new IllegalStateException("UsuarioRepository no disponible");
        return usuarioRepository.existsByCorreo(correo);
    }

    @Transactional
    public void registrarUsuario(Usuario usuario) {
        PasswordEncoder encoder = passwordEncoderProvider.getIfAvailable();
        if (encoder == null) {
            throw new IllegalStateException("No hay PasswordEncoder disponible. Revisa PasswordConfig/WebSecurityConfig.");
        }

        // ✅ en registro siempre llega password en claro
        usuario.setPassword(encoder.encode(usuario.getPassword()));

        if (isMongo()) {
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");
            if (usuarioMongoRepository.existsByCorreo(usuario.getCorreo())) return;

            UsuarioMongo um = new UsuarioMongo();
            um.setNombre(usuario.getNombre());
            um.setCorreo(usuario.getCorreo());
            um.setPassword(usuario.getPassword());

            usuarioMongoRepository.save(um);
            return;
        }

        if (usuarioRepository == null) throw new IllegalStateException("UsuarioRepository no disponible");
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) return;

        usuarioRepository.save(usuario);
    }
}