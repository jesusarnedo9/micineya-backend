package com.arnedo.micine.service;

import com.arnedo.micine.dto.RegistroRequest;
import com.arnedo.micine.dto.ResenaRequest;
import com.arnedo.micine.entity.Resena;
import com.arnedo.micine.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgresoServiceTests {
    @Autowired ProgresoService progreso;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository cuentas;
    @Autowired ResenaService resenas;
    @Autowired ResenaRepository opiniones;
    @Autowired PeliculaRepository peliculas;
    @Autowired ComunidadService comunidad;
    @Autowired MockMvc mvc;

    @Test
    void baldesDeDiezSinPerderElTotal() {
        for (int vistas : new int[]{0, 1, 9, 10, 11, 19, 20, 21, 100, 127}) {
            var p = ProgresoService.calcular(1L, vistas);
            assertThat(p.peliculasVistas()).isEqualTo(vistas);
            assertThat(p.capacidadBalde()).isEqualTo(10);
            assertThat(p.baldesCompletos()).isEqualTo(vistas / 10);
            assertThat(p.pochoclosEnBalde()).isEqualTo(vistas % 10);
            assertThat(p.numeroBalde()).isEqualTo(vistas / 10 + 1);
        }
    }

    @Test
    void editarNoSumaYDeshacerReduceElBalde() {
        var cuenta = crear();
        for (long id = 880010L; id < 880020L; id++) guardar(cuenta.email, id);
        assertThat(progreso.propio(cuenta.email).baldesCompletos()).isEqualTo(1);
        guardar(cuenta.email, 880010L);
        assertThat(progreso.propio(cuenta.email).peliculasVistas()).isEqualTo(10);
        resenas.marcarComoNoVista(cuenta.email, 880010L);
        var ajustado = progreso.propio(cuenta.email);
        assertThat(ajustado.baldesCompletos()).isZero();
        assertThat(ajustado.pochoclosEnBalde()).isEqualTo(9);
        guardar(cuenta.email, 880010L);
        assertThat(progreso.propio(cuenta.email).peliculasVistas()).isEqualTo(10);
    }

    @Test
    void duplicadosHistoricosNoSumanNiLasGuardadas() {
        var cuenta = crear();
        guardar(cuenta.email, 881000L);
        var usuario = cuentas.findByEmail(cuenta.email).orElseThrow();
        var pelicula = peliculas.findByTmdbId(881000L).orElseThrow();
        opiniones.save(new Resena(3, "Duplicado histórico", usuario, pelicula));
        usuario.getPeliculasFavoritas().add(pelicula);
        var soloGuardada = peliculas.save(new com.arnedo.micine.entity.Pelicula(881001L, "Privada", null));
        usuario.getPeliculasFavoritas().add(soloGuardada);
        assertThat(progreso.propio(cuenta.email).peliculasVistas()).isEqualTo(1);
    }

    @Test
    void progresoPropioAutenticadoYPublicoRespetaBloqueos() throws Exception {
        var autor = crear(); var visitante = crear();
        guardar(autor.email, 882000L);
        mvc.perform(get("/api/users/me/progreso")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me/progreso").header("Authorization", "Bearer " + autor.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.peliculasVistas").value(1));
        mvc.perform(get("/api/users/me/progreso").header("Authorization", "Bearer " + visitante.token))
                .andExpect(jsonPath("$.peliculasVistas").value(0));
        mvc.perform(get("/api/comunidad/perfiles/" + autor.id).header("Authorization", "Bearer " + visitante.token))
                .andExpect(status().isNotFound());
        comunidad.aceptar(autor.email, ComunidadService.VERSION_NORMAS);
        mvc.perform(get("/api/comunidad/perfiles/" + autor.id).header("Authorization", "Bearer " + visitante.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.progreso.peliculasVistas").value(1))
                .andExpect(jsonPath("$.progreso.pochoclosEnBalde").value(1));
        comunidad.bloquear(autor.email, visitante.id, true);
        mvc.perform(get("/api/comunidad/perfiles/" + autor.id).header("Authorization", "Bearer " + visitante.token))
                .andExpect(status().isNotFound());
    }

    @Test
    void insigniasEspecialesExigenTodasLasTemporadas() {
        var insignias = ProgresoService.insignias(Map.of(
                1399L, Set.of(1, 2, 3, 4, 5, 6, 7, 8),
                1396L, Set.of(1, 2, 3, 4),
                70523L, Set.of(1, 2, 3)));

        assertThat(insignias).containsExactly("SERIE_TRONOS", "SERIE_CICLO");
    }

    @Test
    void titulosCinefilosSeDesbloqueanConTresCincoYDiezBaldes() {
        assertThat(ProgresoService.calcular(1L, 29).tituloCinefilo()).isNull();
        assertThat(ProgresoService.calcular(1L, 30).tituloCinefilo()).isEqualTo("Cineasta entusiasta");
        assertThat(ProgresoService.calcular(1L, 50).tituloCinefilo()).isEqualTo("Comprometido con el cine");
        assertThat(ProgresoService.calcular(1L, 100).tituloCinefilo()).isEqualTo("Maestro del Cine");
    }

    private record Cuenta(String email, Long id, String token) {}
    private Cuenta crear() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var registro = new RegistroRequest(); registro.setUsername("pop" + suffix);
        registro.setEmail("pop" + suffix + "@example.com"); registro.setPassword("password-segura");
        var auth = usuarios.registrar(registro);
        return new Cuenta(registro.getEmail(), cuentas.findByEmail(registro.getEmail()).orElseThrow().getId(), auth.getToken());
    }
    private void guardar(String email, Long id) {
        var r = new ResenaRequest(); r.setTmdbId(id); r.setTitulo("Película de prueba"); r.setCalificacion(4); r.setComentario("Una opinión");
        resenas.guardarResena(email, r);
    }
}
