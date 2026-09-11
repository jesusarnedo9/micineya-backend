package com.arnedo.micine.service;

import com.arnedo.micine.config.DataSeeder;
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
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoSeriesTests {
    @Autowired RestTemplate http;
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository cuentas;
    @Autowired GeneroRepository generos;
    @Autowired PlataformaRepository plataformas;
    @Autowired ResenaService resenas;
    @Autowired ProgresoService progreso;
    @Autowired CatalogoSeriesService catalogo;
    @Autowired DataSeeder seeder;
    @Autowired PeliculaRepository contenidos;
    @Autowired ResenaRepository opiniones;
    @Autowired EntityManager em;
    MockRestServiceServer tmdb;
    org.springframework.http.client.ClientHttpRequestFactory originalFactory;

    @BeforeEach void setup() {
        originalFactory = http.getRequestFactory();
        tmdb = MockRestServiceServer.bindTo(http).build();
    }
    @AfterEach void cleanup() {
        try { tmdb.verify(); } finally { http.setRequestFactory(originalFactory); }
    }

    @Test
    void serieYMovieConElMismoNumeroNoSeMezclanNiSeModificaElProgreso() throws Exception {
        var cuenta = crear("Acción");
        var r = new ResenaRequest(); r.setTmdbId(123L); r.setTitulo("Película distinta"); r.setCalificacion(4);
        resenas.guardarResena(cuenta.email, r);
        usuarios.descartarPelicula(cuenta.email, 123L);
        usuarios.registrarRecomendaciones(cuenta.email, List.of(123L));
        tmdb.expect(requestTo(containsString("/discover/tv?")))
                .andExpect(queryParam("watch_region", "AR"))
                .andExpect(queryParam("with_watch_providers", "8"))
                .andExpect(queryParam("with_watch_monetization_types", "flatrate"))
                .andExpect(queryParam("with_genres", "10759"))
                .andRespond(withSuccess("""
                    {"page":1,"total_pages":1,"results":[{"id":123,"name":"Serie de prueba",
                    "poster_path":"/serie.jpg","vote_average":8.1,"overview":"Descripción"}]}
                    """, MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo(containsString("/tv/123/watch/providers?")))
                .andRespond(withSuccess("""
                    {"results":{"AR":{"flatrate":[{"provider_id":8,"provider_name":"Netflix","display_priority":1}]}}}
                    """, MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo(containsString("/tv/123/videos?")))
                .andExpect(queryParam("language", "es-MX"))
                .andRespond(withSuccess("""
                    {"results":[{"key":"trailer-latino","name":"Trailer latino","type":"Trailer",
                    "site":"YouTube","official":true,"iso_639_1":"es","iso_3166_1":"MX"}]}
                    """, MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/series/catalogo").header("Authorization", "Bearer " + cuenta.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].id").value(123))
                .andExpect(jsonPath("$.results[0].mediaType").value("tv"))
                .andExpect(jsonPath("$.results[0].title").value("Serie de prueba"))
                .andExpect(jsonPath("$.results[0].plataformas[0]").value("Netflix"))
                .andExpect(jsonPath("$.results[0].videoKey").value("trailer-latino"));
        assertThat(progreso.propio(cuenta.email).peliculasVistas()).isEqualTo(1);
        assertThat(usuarios.getPerfilRecomendacion(cuenta.email).peliculasDescartadasIds()).containsExactly(123L);
        assertThat(TipoContenido.SERIE.clave(123L)).isNotEqualTo(TipoContenido.PELICULA.clave(123L));
    }

    @Test
    void sinEquivalenciaNoReemplazaTerrorPorOtroGeneroNiLlamaATmdb() {
        var cuenta = crear("Terror");
        var resultado = catalogo.consultar(cuenta.email);
        assertThat(resultado.results()).isEmpty();
        assertThat(resultado.generosSinEquivalencia()).containsExactly("Terror");
    }

    @Test
    void sinPlataformasNoAmpliaElCatalogoSilenciosamente() {
        var cuenta = crear("Comedia");
        cuentas.findByEmail(cuenta.email).orElseThrow().getPlataformas().clear();
        assertThat(catalogo.consultar(cuenta.email).results()).isEmpty();
    }

    @Test
    void seedRepetibleConservaIdsYPreferenciasExistentes() throws Exception {
        var cuenta = crear("Acción");
        var idsAntes = generos.findAll().stream().map(Genero::getId).sorted().toList();
        var accion = generos.findByNombre("Acción").orElseThrow();
        Long idOriginal = accion.getId();
        accion.setTmdbTvId(null); // Simula la nueva columna en un género que ya existía.
        seeder.run(); seeder.run(); em.flush(); em.clear();
        assertThat(generos.findAll().stream().map(Genero::getId).sorted().toList()).isEqualTo(idsAntes);
        assertThat(generos.count()).isEqualTo(19);
        assertThat(generos.findById(idOriginal).orElseThrow().getTmdbTvId()).isEqualTo(10759);
        assertThat(cuentas.findByEmail(cuenta.email).orElseThrow().getGeneros())
                .extracting(Genero::getId).containsExactly(idOriginal);
    }

    @Test
    void endpointsExigenSesionEIdsInvalidosNoLleganATmdb() throws Exception {
        for (String path : List.of("/api/series/catalogo", "/api/series/123/temporadas",
                "/api/series/123/temporadas/1")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        var cuenta = crear("Comedia");
        mvc.perform(get("/api/series/0/temporadas").header("Authorization", "Bearer " + cuenta.token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/series/123/temporadas/0").header("Authorization", "Bearer " + cuenta.token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void temporadasSeDeserializanConFechasYExcluyenEspeciales() throws Exception {
        var cuenta = crear("Comedia");
        tmdb.expect(requestTo(containsString("/tv/123?"))).andRespond(withSuccess("""
            {"id":123,"name":"Serie","status":"Ended","poster_path":"/serie.jpg","seasons":[
            {"id":1,"season_number":0,"name":"Especiales","episode_count":1,"air_date":null},
            {"id":2,"season_number":1,"name":"Temporada 1","episode_count":2,"air_date":"2020-01-01"}]}
            """, MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/series/123/temporadas").header("Authorization", "Bearer " + cuenta.token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mediaType").value("tv"))
                .andExpect(jsonPath("$.temporadas.length()").value(1))
                .andExpect(jsonPath("$.temporadas[0].numero").value(1))
                .andExpect(jsonPath("$.temporadas[0].estreno").value("2020-01-01"));
    }

    @Test
    void renovarSeriesExcluyeVistasDescartadasYLoteActualSinUsarElHistorialDePeliculas() throws Exception {
        var cuenta = crear("Acción");
        var usuario = cuentas.findByEmail(cuenta.email).orElseThrow();
        var serie = new Pelicula(870001L, "Serie vista", null); serie.setMediaType(TipoContenido.SERIE);
        contenidos.saveAndFlush(serie);
        var resena = new Resena(4, "", usuario, serie); resena.getTemporadasVistas().add(1);
        opiniones.saveAndFlush(resena);
        catalogo.descartar(cuenta.email, 870002L, true);
        usuarios.descartarPelicula(cuenta.email, 870004L);
        java.util.stream.LongStream.rangeClosed(1, 50).forEach(id -> usuario.getSeriesRecientes()
                .put(id, java.time.LocalDateTime.now().minusDays(2)));
        tmdb.expect(requestTo(containsString("/discover/tv?"))).andRespond(withSuccess("""
            {"page":1,"total_pages":1,"results":[{"id":870001,"name":"Vista"},
            {"id":870002,"name":"Descartada"},{"id":870003,"name":"Lote anterior"},{"id":870004,"name":"Nueva"}]}
            """, MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo(containsString("/tv/870004/watch/providers?")))
                .andRespond(withSuccess("""
                    {"results":{"AR":{"flatrate":[{"provider_id":8,"provider_name":"Netflix","display_priority":1}]}}}
                    """, MediaType.APPLICATION_JSON));
        tmdb.expect(requestTo(containsString("/tv/870004/videos?"))).andRespond(withSuccess("""
            {"results":[{"key":"trailer","name":"Trailer latino","type":"Trailer","site":"YouTube"}]}
            """, MediaType.APPLICATION_JSON));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/series/recomendadas/renovar")
                        .header("Authorization", "Bearer " + cuenta.token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualesIds\":[870003]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].id").value(870004));
        em.flush(); em.clear();
        var guardado = cuentas.findByEmail(cuenta.email).orElseThrow();
        assertThat(guardado.getSeriesRecientes()).hasSize(50).containsKey(870004L);
        assertThat(guardado.getRecomendacionesRecientes()).isEmpty();
        assertThat(guardado.getPeliculasDescartadas()).containsKey(870004L);
    }

    private record Cuenta(String email, String token) {}
    private Cuenta crear(String genero) {
        String nombre = "tv" + UUID.randomUUID().toString().substring(0, 8);
        var r = new RegistroRequest(); r.setUsername(nombre); r.setEmail(nombre + "@example.com");
        r.setPassword("password-de-prueba");
        var auth = usuarios.registrar(r);
        var usuario = cuentas.findByEmail(r.getEmail()).orElseThrow();
        usuario.getGeneros().add(generos.findByNombre(genero).orElseThrow());
        usuario.getPlataformas().add(plataformas.findByNombre("Netflix").orElseThrow());
        return new Cuenta(r.getEmail(), auth.getToken());
    }
}
