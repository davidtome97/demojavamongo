package com.sistemagestionapp.demojava.service;

import com.sistemagestionapp.demojava.model.Producto;
import com.sistemagestionapp.demojava.model.Usuario;
import com.sistemagestionapp.demojava.model.mongo.ProductoMongo;
import com.sistemagestionapp.demojava.model.mongo.UsuarioMongo;
import com.sistemagestionapp.demojava.repository.jpa.ProductoRepository;
import com.sistemagestionapp.demojava.repository.mongo.ProductoMongoRepository;
import com.sistemagestionapp.demojava.repository.mongo.UsuarioMongoRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;            // null si mongo
    private final ProductoMongoRepository productoMongoRepository;  // null si sql
    private final UsuarioMongoRepository usuarioMongoRepository;    // para obtener el usuarioId de mongo
    private final String dbEngine;

    public ProductoService(
            ObjectProvider<ProductoRepository> productoRepository,
            ObjectProvider<ProductoMongoRepository> productoMongoRepository,
            ObjectProvider<UsuarioMongoRepository> usuarioMongoRepository,
            @Value("${DB_ENGINE:mysql}") String dbEngine
    ) {
        this.productoRepository = productoRepository.getIfAvailable();
        this.productoMongoRepository = productoMongoRepository.getIfAvailable();
        this.usuarioMongoRepository = usuarioMongoRepository.getIfAvailable();
        this.dbEngine = (dbEngine == null || dbEngine.isBlank()) ? "mysql" : dbEngine.toLowerCase();
    }

    private boolean isMongo() {
        return "mongo".equalsIgnoreCase(dbEngine);
    }

    // =========================================================
    // LISTAR (SOLO DEL USUARIO)
    // =========================================================
    @Transactional(readOnly = true)
    public List<?> listarPorUsuario(Usuario usuarioSql, String correoUsuario) {
        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible");
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");

            UsuarioMongo um = usuarioMongoRepository.findByCorreo(correoUsuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en Mongo: " + correoUsuario));

            return productoMongoRepository.findByUsuarioId(um.getId());
        }

        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible");
        if (usuarioSql == null) throw new IllegalArgumentException("usuarioSql es obligatorio en SQL");

        return productoRepository.findByPropietario(usuarioSql);
    }

    // =========================================================
    // BUSCAR POR ID (SOLO SI ES DEL USUARIO)
    // =========================================================
    @Transactional(readOnly = true)
    public Object buscarPorIdDeUsuario(String id, Usuario usuarioSql, String correoUsuario) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id es obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible");
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");

            UsuarioMongo um = usuarioMongoRepository.findByCorreo(correoUsuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en Mongo: " + correoUsuario));

            return productoMongoRepository.findByIdAndUsuarioId(id, um.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o no pertenece al usuario: " + id));
        }

        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible");
        if (usuarioSql == null) throw new IllegalArgumentException("usuarioSql es obligatorio en SQL");

        Long longId = parseLongId(id);
        return productoRepository.findByIdAndPropietario(longId, usuarioSql)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado o no pertenece al usuario: " + id));
    }

    // =========================================================
    // GUARDAR/ACTUALIZAR (SIEMPRE ASIGNANDO USUARIO)
    // =========================================================
    @Transactional
    public void guardar(String id, String nombre, String descripcion, double precio, Usuario usuarioSql, String correoUsuario) {

        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible");
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");

            UsuarioMongo um = usuarioMongoRepository.findByCorreo(correoUsuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en Mongo: " + correoUsuario));

            final ProductoMongo p;

            if (id == null || id.isBlank()) {
                p = new ProductoMongo();
                p.setUsuarioId(um.getId());
            } else {
                p = productoMongoRepository.findByIdAndUsuarioId(id, um.getId())
                        .orElseThrow(() -> new IllegalArgumentException("No puedes editar un producto que no es tuyo: " + id));
            }

            p.setNombre(nombre);
            p.setDescripcion(descripcion);
            p.setPrecio(precio);

            productoMongoRepository.save(p);
            return;
        }

        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible");
        if (usuarioSql == null) throw new IllegalArgumentException("usuarioSql es obligatorio en SQL");

        final Producto p;

        if (id == null || id.isBlank()) {
            p = new Producto();
            p.setPropietario(usuarioSql); // ✅ en SQL
        } else {
            Long longId = parseLongId(id);
            p = productoRepository.findByIdAndPropietario(longId, usuarioSql)
                    .orElseThrow(() -> new IllegalArgumentException("No puedes editar un producto que no es tuyo: " + id));
        }

        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);

        productoRepository.save(p);
    }

    // =========================================================
    // BORRAR (SOLO SI ES DEL USUARIO)
    // =========================================================
    @Transactional
    public void borrarPorId(String id, Usuario usuarioSql, String correoUsuario) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id es obligatorio");

        if (isMongo()) {
            if (productoMongoRepository == null) throw new IllegalStateException("ProductoMongoRepository no disponible");
            if (usuarioMongoRepository == null) throw new IllegalStateException("UsuarioMongoRepository no disponible");

            UsuarioMongo um = usuarioMongoRepository.findByCorreo(correoUsuario)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en Mongo: " + correoUsuario));

            ProductoMongo p = productoMongoRepository.findByIdAndUsuarioId(id, um.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No puedes borrar un producto que no es tuyo: " + id));

            productoMongoRepository.delete(p);
            return;
        }

        if (productoRepository == null) throw new IllegalStateException("ProductoRepository no disponible");
        if (usuarioSql == null) throw new IllegalArgumentException("usuarioSql es obligatorio en SQL");

        Long longId = parseLongId(id);

        Producto p = productoRepository.findByIdAndPropietario(longId, usuarioSql)
                .orElseThrow(() -> new IllegalArgumentException("No puedes borrar un producto que no es tuyo: " + id));

        productoRepository.delete(p);
    }

    private Long parseLongId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido (se esperaba numérico para SQL): " + id);
        }
    }
}