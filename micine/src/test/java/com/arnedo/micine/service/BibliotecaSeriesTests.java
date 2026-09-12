package com.arnedo.micine.service;

import com.arnedo.micine.dto.*;
import com.arnedo.micine.entity.*;
import com.arnedo.micine.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BibliotecaSeriesTests {
    @Autowired BibliotecaService biblioteca;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository cuentas;
    @Autowired PeliculaRepository contenidos;
    @Autowired ResenaRepository resenas;
    @Autowired ResenaService peliculas;
    @Autowired ProgresoService progreso;
    @Autowired ComunidadService comunidad;
    @Autowired CatalogoSeriesService catalogo;
    @Autowired CuentaService cuentaService;
    @Autowired EntityManager em;
    @Autowired RestTemplate http;
    @Autowired MockMvc mvc;
    static final AtomicLong IDS = new AtomicLong(991000);
    MockRestServiceServer tmdb;
    org.springframework.http.client.ClientHttpRequestFactory original;

    @BeforeEach void setup() { original = http.getRequestFactory(); tmdb = MockRestServiceServer.bindTo(http).ignoreExpectOrder(true).build(); }
    @AfterEach void cleanup() { try { tmdb.verify(); } finally { http.setRequestFactory(original); } }

    @Test
    void peliculaYSerieMismoIdConservanResenasYGuardadasSeparadas() {
        var cuenta = crear(); long id = IDS.incrementAndGet();
        var pelicula = new ResenaRequest(); pelicula.setTmdbId(id); pelicula.setTitulo("Una película"); pelicula.setCalificacion(3);
        peliculas.guardarResena(cuenta.email, pelicula);
        prepararTemporadas(id, 1);
        var serie = biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Una serie"));
        assertThat(serie.mediaType()).isEqualTo(TipoContenido.SERIE);
        assertThat(biblioteca.mias(cuenta.email)).hasSize(2);
        assertThat(peliculas.obtenerMisResenas(cuenta.email)).hasSize(1);
        assertThat(usuarios.getPerfilRecomendacion(cuenta.email).peliculasVistasIds()).containsExactly(id);
        biblioteca.guardar(cuenta.email, new BibliotecaDtos.Guardar(TipoContenido.SERIE, id, "Ignorado", null));
        biblioteca.guardar(cuenta.email, new BibliotecaDtos.Guardar(TipoContenido.PELICULA, id, "Una película", null));
        assertThat(biblioteca.favoritas(cuenta.email)).hasSize(2);
        assertThat(usuarios.getPeliculasFavoritas(cuenta.email)).hasSize(1);
        usuarios.eliminarPeliculaFavorita(cuenta.email, id);
        peliculas.marcarComoNoVista(cuenta.email, id);
        assertThat(biblioteca.favoritas(cuenta.email)).extracting(BibliotecaDtos.Favorita::mediaType).containsExactly(TipoContenido.SERIE);
        assertThat(biblioteca.mias(cuenta.email)).extracting(ResenaResponse::mediaType).containsExactly(TipoContenido.SERIE);
        assertThat(progreso.propio(cuenta.email).totalPochoclos()).isEqualTo(1);
    }

    @Test
    void cadaTemporadaCuentaUnaVezEditarYDeshacerNoDuplicanPochoclos() {
        var cuenta = crear(); long id = IDS.incrementAndGet();
        prepararTemporadas(id, 1);
        prepararTemporadas(id, 2);
        var primera = biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Primera"));
        var editada = biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Editada"));
        assertThat(editada.id()).isEqualTo(primera.id());
        assertThat(editada.fechaVista()).isEqualTo(primera.fechaVista());
        var segunda = biblioteca.resenar(cuenta.email, resena(id, Set.of(2), "Segunda"));
        assertThat(segunda.id()).isNotEqualTo(primera.id());
        assertThat(biblioteca.mias(cuenta.email)).extracting(ResenaResponse::numeroTemporada)
                .containsExactlyInAnyOrder(1, 2);
        var p = progreso.propio(cuenta.email);
        assertThat(p.peliculasVistas()).isZero(); assertThat(p.seriesVistas()).isEqualTo(1);
        assertThat(p.temporadasVistas()).isEqualTo(2); assertThat(p.totalPochoclos()).isEqualTo(2);
        biblioteca.marcarTemporadaNoVista(cuenta.email, id, 1);
        assertThat(progreso.propio(cuenta.email).totalPochoclos()).isEqualTo(1);
        biblioteca.marcarNoVista(cuenta.email, TipoContenido.SERIE, id);
        biblioteca.marcarNoVista(cuenta.email, TipoContenido.SERIE, id);
        assertThat(progreso.propio(cuenta.email).totalPochoclos()).isZero();
        assertThat(biblioteca.mias(cuenta.email)).isEmpty();
    }

    @Test
    void marcarComoVistaQuitaLaPeliculaDeGuardadas() {
        var cuenta = crear(); long id = IDS.incrementAndGet();
        biblioteca.guardar(cuenta.email,
                new BibliotecaDtos.Guardar(TipoContenido.PELICULA, id, "Guardada", null));
        assertThat(biblioteca.favoritas(cuenta.email)).hasSize(1);

        biblioteca.resenar(cuenta.email, new BibliotecaDtos.Resenar(
                TipoContenido.PELICULA, id, "Guardada", null, 4, "Vista", false, Set.of()));

        assertThat(biblioteca.favoritas(cuenta.email)).isEmpty();
    }

    @Test
    void completarDiezTemporadasLlenaUnBaldeYDesmarcarLoAjusta() {
        var cuenta = crear(); long id = IDS.incrementAndGet();
        for (int numero = 1; numero <= 10; numero++) prepararTemporadas(id, numero);
        biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Temporada 1"));
        biblioteca.guardar(cuenta.email, new BibliotecaDtos.Guardar(TipoContenido.SERIE, id, "Serie", null));
        for (int numero = 2; numero <= 10; numero++) {
            biblioteca.resenar(cuenta.email, resena(id, Set.of(numero), "Temporada " + numero));
        }
        assertThat(progreso.propio(cuenta.email).baldesCompletos()).isEqualTo(1);
        assertThat(progreso.propio(cuenta.email).pochoclosEnBalde()).isZero();
        assertThat(biblioteca.favoritas(cuenta.email)).isEmpty();
        biblioteca.marcarTemporadaNoVista(cuenta.email, id, 10);
        assertThat(progreso.propio(cuenta.email).baldesCompletos()).isZero();
        assertThat(progreso.propio(cuenta.email).pochoclosEnBalde()).isEqualTo(9);
        assertThat(biblioteca.favoritas(cuenta.email)).hasSize(1);
    }

    @Test
    void errorDeTemporadaNoGuardaResenaNiOtorgaCredito() {
        var cuenta = crear(); long id = IDS.incrementAndGet();
        tmdb.expect(requestTo(containsString("/tv/" + id + "?"))).andRespond(withSuccess(detalle(id), MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo(containsString("/season/1?"))).andRespond(withSuccess("""
            {"season_number":1,"episodes":[{"id":101,"episode_number":1,"air_date":"2099-01-01"},
            {"id":102,"episode_number":2,"air_date":null}]}
            """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "No estrenada")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(biblioteca.mias(cuenta.email)).isEmpty();
        assertThat(progreso.propio(cuenta.email).totalPochoclos()).isZero();
        assertThat(contenidos.findByMediaTypeAndTmdbId(TipoContenido.SERIE, id)).isEmpty();
    }

    @Test
    void contratoValidaTipoTemporadasYPuntajeYExigeSesion() throws Exception {
        mvc.perform(get("/api/biblioteca/resenas")).andExpect(status().isUnauthorized());
        var cuenta = crear();
        for (String json : List.of(
                "{\"mediaType\":\"desconocido\",\"tmdbId\":1}",
                "{\"mediaType\":\"tv\",\"tmdbId\":1,\"calificacion\":4,\"temporadasVistas\":[]}",
                "{\"mediaType\":\"tv\",\"tmdbId\":1,\"calificacion\":4,\"temporadasVistas\":[1,2]}",
                "{\"mediaType\":\"tv\",\"tmdbId\":1,\"calificacion\":4,\"temporadasVistas\":[0]}",
                "{\"mediaType\":\"movie\",\"tmdbId\":1,\"calificacion\":4,\"temporadasVistas\":[1]}",
                "{\"mediaType\":\"tv\",\"tmdbId\":1,\"calificacion\":6,\"temporadasVistas\":[1]}")) {
            mvc.perform(post("/api/biblioteca/resenas").header("Authorization", "Bearer " + cuenta.token)
                    .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void perfilMixtoOrdenaPorVistaNoPorEdicionYCuentaAjenaNoPuedeBorrarla() {
        var cuenta = crear(); var otra = crear(); long id = IDS.incrementAndGet();
        prepararTemporadas(id, 1);
        var serie = biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Antigua"));
        resenas.findById(serie.id()).orElseThrow().setFechaVista(LocalDateTime.now().minusDays(2));
        biblioteca.resenar(cuenta.email, new BibliotecaDtos.Resenar(TipoContenido.PELICULA,
                IDS.incrementAndGet(), "Reciente", null, 3, "", false, Set.of()));
        biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Editada hoy"));
        assertThat(biblioteca.mias(cuenta.email)).extracting(ResenaResponse::mediaType)
                .containsExactly(TipoContenido.PELICULA, TipoContenido.SERIE);
        biblioteca.marcarNoVista(otra.email, TipoContenido.SERIE, id);
        assertThat(biblioteca.mias(cuenta.email)).hasSize(2);
        assertThat(biblioteca.mias(otra.email)).isEmpty();
    }

    @Test
    void comunidadComparteResenaDeSerieNoGuardadasYRespetaBloqueo() throws Exception {
        var autor = crear(); var lector = crear(); long id = IDS.incrementAndGet();
        prepararTemporadas(id, 1);
        biblioteca.resenar(autor.email, resena(id, Set.of(1), "Recomendable"));
        biblioteca.guardar(autor.email, new BibliotecaDtos.Guardar(TipoContenido.SERIE, id, "", null));
        comunidad.aceptar(autor.email, ComunidadService.VERSION_NORMAS);
        mvc.perform(get("/api/comunidad/perfiles/" + autor.id).header("Authorization", "Bearer " + lector.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publicaciones[0].mediaType").value("tv"))
                .andExpect(jsonPath("$.publicaciones[0].numeroTemporada").value(1))
                .andExpect(jsonPath("$.progreso.temporadasVistas").value(1))
                .andExpect(jsonPath("$.favoritas").doesNotExist());
        comunidad.bloquear(autor.email, lector.id, true);
        mvc.perform(get("/api/comunidad/perfiles/" + autor.id).header("Authorization", "Bearer " + lector.token))
                .andExpect(status().isNotFound());
    }

    @Test
    void descartesDeSeriesSonPrivadosIndependientesYSeEliminanConLaCuenta() {
        var cuenta = crear(); var otra = crear(); long id = IDS.incrementAndGet();
        catalogo.descartar(cuenta.email, id, true);
        usuarios.descartarPelicula(cuenta.email, id);
        assertThat(cuentas.findByEmail(otra.email).orElseThrow().getSeriesDescartadas()).isEmpty();
        catalogo.descartar(cuenta.email, id, false);
        assertThat(usuarios.getPerfilRecomendacion(cuenta.email).peliculasDescartadasIds()).containsExactly(id);
        prepararTemporadas(id, 1);
        biblioteca.resenar(cuenta.email, resena(id, Set.of(1), "Adiós"));
        biblioteca.guardar(cuenta.email, new BibliotecaDtos.Guardar(TipoContenido.SERIE, id, "", null));
        catalogo.descartar(cuenta.email, id, true);
        cuentaService.eliminar(cuenta.email, "password-de-prueba"); em.flush();
        assertThat(cuentas.findByEmail(cuenta.email)).isEmpty();
        assertThat(resenas.findByUsuarioEmail(cuenta.email)).isEmpty();
    }

    private void prepararTemporadas(long id, int... numeros) {
        tmdb.expect(requestTo(containsString("/tv/" + id + "?"))).andRespond(withSuccess(detalle(id), MediaType.APPLICATION_JSON));
        // Set.copyOf no promete orden. Para múltiples temporadas se validan URLs en cualquier orden.
        for (int n : numeros) {
            tmdb.expect(requestTo(containsString("/tv/" + id + "/season/")))
                    .andRespond(request -> {
                        String path = request.getURI().getPath();
                        int numero = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                        assertThat(numeros).contains(numero);
                        return withSuccess("{\"season_number\":" + numero + ",\"episodes\":["
                                + "{\"id\":101,\"episode_number\":1,\"air_date\":\"2020-01-01\"},"
                                + "{\"id\":102,\"episode_number\":2,\"air_date\":\"2020-01-02\"}]}",
                                MediaType.APPLICATION_JSON).createResponse(request);
                    });
        }
    }
    private String detalle(long id) {
        String seasons = java.util.stream.IntStream.rangeClosed(1, 10).mapToObj(n ->
                "{\"season_number\":" + n + ",\"name\":\"Temporada " + n
                        + "\",\"episode_count\":2,\"air_date\":\"2020-01-01\"}")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"id\":" + id + ",\"name\":\"Serie de prueba\",\"status\":\"Returning Series\",\"seasons\":[" + seasons + "]}";
    }
    private BibliotecaDtos.Resenar resena(long id, Set<Integer> temporadas, String comentario) {
        return new BibliotecaDtos.Resenar(TipoContenido.SERIE, id, "No confiar en este título", null, 4, comentario, false, temporadas);
    }
    private record Cuenta(String email, Long id, String token) {}
    private Cuenta crear() {
        String nombre = "lib" + UUID.randomUUID().toString().substring(0, 8);
        var r = new RegistroRequest(); r.setUsername(nombre); r.setEmail(nombre + "@example.com"); r.setPassword("password-de-prueba");
        var auth = usuarios.registrar(r);
        return new Cuenta(r.getEmail(), cuentas.findByEmail(r.getEmail()).orElseThrow().getId(), auth.getToken());
    }
}
