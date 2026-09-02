package com.arnedo.micine.service;

import com.arnedo.micine.dto.RegistroRequest;
import com.arnedo.micine.dto.ResenaRequest;
import com.arnedo.micine.dto.ResenaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ResenaServiceTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ResenaService resenaService;

    @Test
    void volverAGuardarLaMismaPeliculaActualizaLaResenaSinDuplicarla() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "resena_" + suffix + "@example.com";

        RegistroRequest registro = new RegistroRequest();
        registro.setUsername("resena_" + suffix);
        registro.setEmail(email);
        registro.setPassword("password-segura");
        usuarioService.registrar(registro);

        resenaService.guardarResena(email, resena(550L, 3, "Primera opinión"));
        ResenaResponse actualizada = resenaService.guardarResena(
                email, resena(550L, 5, "Ahora me encantó"));

        List<ResenaResponse> propias = resenaService.obtenerMisResenas(email);

        assertThat(propias).hasSize(1);
        assertThat(actualizada.calificacion()).isEqualTo(5);
        assertThat(actualizada.comentario()).isEqualTo("Ahora me encantó");
        assertThat(propias.getFirst().id()).isEqualTo(actualizada.id());
    }

    @Test
    void marcarComoNoVistaEliminaLaResenaDelPerfil() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "deshacer_" + suffix + "@example.com";

        RegistroRequest registro = new RegistroRequest();
        registro.setUsername("deshacer_" + suffix);
        registro.setEmail(email);
        registro.setPassword("password-segura");
        usuarioService.registrar(registro);
        resenaService.guardarResena(email, resena(680L, 4, "Marcada por error"));

        resenaService.marcarComoNoVista(email, 680L);

        assertThat(resenaService.obtenerMisResenas(email)).isEmpty();
    }

    private ResenaRequest resena(Long tmdbId, int calificacion, String comentario) {
        ResenaRequest request = new ResenaRequest();
        request.setTmdbId(tmdbId);
        request.setTitulo("El club de la pelea");
        request.setPosterPath("/poster.jpg");
        request.setCalificacion(calificacion);
        request.setComentario(comentario);
        return request;
    }
}
