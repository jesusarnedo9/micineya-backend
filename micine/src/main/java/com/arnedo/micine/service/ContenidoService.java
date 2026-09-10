package com.arnedo.micine.service;

import com.arnedo.micine.dto.TipoContenido;
import com.arnedo.micine.entity.Pelicula;
import com.arnedo.micine.repository.PeliculaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContenidoService {
    private final PeliculaRepository contenidos;
    private final TransactionTemplate creacion;

    public ContenidoService(PeliculaRepository contenidos, PlatformTransactionManager manager) {
        this.contenidos = contenidos;
        this.creacion = new TransactionTemplate(manager);
        this.creacion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Pelicula obtenerOCrear(TipoContenido tipo, Long id, String titulo, String poster) {
        tipo.clave(id);
        var existente = contenidos.findByMediaTypeAndTmdbId(tipo, id);
        if (existente.isPresent()) return existente.get();
        try {
            return creacion.execute(status -> {
                var contenido = new Pelicula(id, titulo == null ? "" : titulo.trim(), poster);
                contenido.setMediaType(tipo);
                return contenidos.saveAndFlush(contenido);
            });
        } catch (DataIntegrityViolationException ex) {
            // Otra cuenta pudo guardar el mismo contenido durante la consulta a TMDB.
            // La transacción fallida ya terminó; no reutilizar una sesión marcada rollback-only.
            return contenidos.findByMediaTypeAndTmdbId(tipo, id).orElseThrow(() -> ex);
        }
    }
}
