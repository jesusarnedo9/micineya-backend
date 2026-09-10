package com.arnedo.micine.service;

import com.arnedo.micine.entity.Usuario;
import com.arnedo.micine.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RecomendacionesUsuarioTests {
    @Autowired private UsuarioService service;
    @Autowired private UsuarioRepository users;
    @Autowired private EntityManager em;

    @Test
    void descartePersisteEsPrivadoSePuedeRepetirYDeshacer() {
        String email = crearUsuario();
        String otro = crearUsuario();
        service.descartarPelicula(email, 123L);
        service.descartarPelicula(email, 123L);
        em.flush();
        em.clear();

        assertThat(service.getPerfilRecomendacion(email).peliculasDescartadasIds()).containsExactly(123L);
        assertThat(service.getPerfilRecomendacion(otro).peliculasDescartadasIds()).isEmpty();
        service.deshacerDescarte(email, 123L);
        service.deshacerDescarte(email, 123L);
        em.flush();
        em.clear();
        assertThat(service.getPerfilRecomendacion(email).peliculasDescartadasIds()).isEmpty();
    }

    @Test
    void descarteVencidoDejaDeExcluirLaPelicula() {
        String email = crearUsuario();
        users.findByEmail(email).orElseThrow().getPeliculasDescartadas()
                .put(123L, LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        em.flush();
        em.clear();
        assertThat(service.getPerfilRecomendacion(email).peliculasDescartadasIds()).isEmpty();
    }

    @Test
    void conservaSoloLasUltimasCincuentaRecomendacionesPorCuenta() {
        String email = crearUsuario();
        String otro = crearUsuario();
        Usuario usuario = users.findByEmail(email).orElseThrow();
        LongStream.rangeClosed(1, 50).forEach(id -> usuario.getRecomendacionesRecientes()
                .put(id, LocalDateTime.now(ZoneOffset.UTC).minusDays(2)));

        service.registrarRecomendaciones(email, LongStream.rangeClosed(51, 60).boxed().toList());
        em.flush();
        em.clear();
        assertThat(service.getPerfilRecomendacion(email).recomendacionesRecientesIds())
                .hasSize(50).containsAll(LongStream.rangeClosed(51, 60).boxed().toList());
        assertThat(service.getPerfilRecomendacion(otro).recomendacionesRecientesIds()).isEmpty();
    }

    private String crearUsuario() {
        Usuario usuario = new Usuario();
        String name = "test_" + UUID.randomUUID();
        usuario.setUsername(name);
        usuario.setEmail(name + "@example.com");
        usuario.setPassword("unused-test-password");
        users.saveAndFlush(usuario);
        return usuario.getEmail();
    }
}
