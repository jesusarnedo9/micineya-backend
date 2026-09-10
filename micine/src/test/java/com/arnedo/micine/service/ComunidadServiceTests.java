package com.arnedo.micine.service;

import com.arnedo.micine.dto.*;
import com.arnedo.micine.dto.ComunidadDtos.*;
import com.arnedo.micine.entity.*;
import com.arnedo.micine.entity.ReporteComunidad.Motivo;
import com.arnedo.micine.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComunidadServiceTests {
    @Autowired ComunidadService comunidad;
    @Autowired UsuarioService cuentas;
    @Autowired CuentaService eliminarCuenta;
    @Autowired ResenaService resenas;
    @Autowired UsuarioRepository usuarios;
    @Autowired RelacionUsuarioRepository relaciones;
    @Autowired ReporteComunidadRepository reportes;
    @Autowired PeliculaRepository peliculas;
    @Autowired MockMvc mvc;
    private record Cuenta(Usuario usuario, String token) { String email() { return usuario.getEmail(); } Long id() { return usuario.getId(); } }

    @Test
    void soloUsernameYPublicosConAceptacionSinDatosPrivados() throws Exception {
        Cuenta yo = crear(true), otro = crear(false);
        var pelicula = peliculas.save(new Pelicula(892345L, "Guardada privada", null));
        otro.usuario().getPeliculasFavoritas().add(pelicula);
        guardar(otro, 892346L, false);
        assertThat(comunidad.buscar(yo.email(), otro.usuario().getUsername())).isEmpty();
        comunidad.aceptar(otro.email(), ComunidadService.VERSION_NORMAS);
        assertThat(comunidad.buscar(yo.email(), otro.usuario().getUsername().toUpperCase())).extracting(Persona::id).containsExactly(otro.id());
        assertThat(comunidad.buscar(yo.email(), otro.email())).isEmpty();
        assertThat(comunidad.buscar(yo.email(), "%_")).isEmpty();
        mvc.perform(get("/api/comunidad/perfiles/" + otro.id()).header("Authorization", "Bearer " + yo.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.persona.username").value(otro.usuario().getUsername()))
                .andExpect(jsonPath("$.persona.email").doesNotExist()).andExpect(jsonPath("$.persona.password").doesNotExist())
                .andExpect(jsonPath("$.persona.seguidores").doesNotExist()).andExpect(jsonPath("$.persona.siguiendo").value(false))
                .andExpect(jsonPath("$.guardadas").doesNotExist()).andExpect(jsonPath("$.preferencias").doesNotExist())
                .andExpect(jsonPath("$.publicaciones.length()").value(1));
    }

    @Test
    void feedSoloSeguidosConSpoilerPaginacionYSeguirIdempotente() {
        Cuenta yo = crear(true), seguido = crear(true), ajeno = crear(true);
        guardar(ajeno, 893000L, false);
        for (long i = 0; i < 21; i++) guardar(seguido, 893100L + i, true);
        assertThat(comunidad.feed(yo.email(), 0).publicaciones()).isEmpty();
        comunidad.seguir(yo.email(), seguido.id(), true);
        comunidad.seguir(yo.email(), seguido.id(), true);
        assertThat(relaciones.findByOrigenIdAndTipo(yo.id(), RelacionUsuario.Tipo.SEGUIR)).hasSize(1);
        Pagina primera = comunidad.feed(yo.email(), 0), segunda = comunidad.feed(yo.email(), 1);
        assertThat(primera.publicaciones()).hasSize(20).allMatch(p -> p.autorId().equals(seguido.id()) && p.spoiler());
        assertThat(primera.hayMas()).isTrue();
        assertThat(segunda.publicaciones()).hasSize(1);
        assertThat(segunda.hayMas()).isFalse();
        assertThat(primera.publicaciones()).extracting(Publicacion::id).doesNotContain(segunda.publicaciones().getFirst().id());
        comunidad.seguir(yo.email(), seguido.id(), false);
        assertThat(comunidad.feed(yo.email(), 0).publicaciones()).isEmpty();
    }

    @Test
    void bloqueoReciprocoOcultaPerfilBusquedaFeedYEndpointAnterior() throws Exception {
        Cuenta a = crear(true), b = crear(true);
        guardar(b, 894000L, false);
        comunidad.seguir(a.email(), b.id(), true);
        comunidad.seguir(b.email(), a.id(), true);
        comunidad.bloquear(a.email(), b.id(), true);
        assertThat(comunidad.feed(a.email(), 0).publicaciones()).isEmpty();
        assertThat(comunidad.buscar(b.email(), a.usuario().getUsername())).isEmpty();
        assertThat(comunidad.misBloqueados(a.email())).extracting(Persona::id).containsExactly(b.id());
        assertThat(comunidad.misBloqueados(b.email())).isEmpty();
        mvc.perform(get("/api/comunidad/perfiles/" + a.id()).header("Authorization", "Bearer " + b.token())).andExpect(status().isNotFound());
        mvc.perform(put("/api/comunidad/siguiendo/" + a.id()).header("Authorization", "Bearer " + b.token())).andExpect(status().isNotFound());
        assertThat(resenas.obtenerResenasPorPelicula(a.email(), 894000L)).isEmpty();
        comunidad.bloquear(a.email(), b.id(), false);
        assertThat(comunidad.perfil(a.email(), b.id(), 0).persona().siguiendo()).isFalse();
        assertThat(comunidad.perfil(b.email(), a.id(), 0).persona().siguiendo()).isFalse();
    }

    @Test
    void reportesSonIdempotentesYModeracionRequierePermisoReal() throws Exception {
        Cuenta yo = crear(true), autor = crear(true), admin = crear(true);
        var resena = guardar(autor, 895000L, false);
        var request = new Reportar(autor.id(), resena.id(), Motivo.ACOSO);
        comunidad.reportar(yo.email(), request);
        comunidad.reportar(yo.email(), request);
        assertThat(reportes.findAll()).hasSize(1);
        mvc.perform(get("/api/comunidad/moderacion").header("Authorization", "Bearer " + yo.token())).andExpect(status().isForbidden());
        @SuppressWarnings("unchecked") Set<Long> admins = (Set<Long>) ReflectionTestUtils.getField(comunidad, "moderadores");
        admins.add(admin.id());
        try {
            var pendientes = comunidad.pendientes(admin.email());
            comunidad.resolver(admin.email(), pendientes.getFirst().id(), Accion.OCULTAR_RESENA);
            assertThat(comunidad.perfil(yo.email(), autor.id(), 0).publicaciones()).isEmpty();
            assertThat(resenas.obtenerMisResenas(autor.email()).getFirst().ocultadaModeracion()).isTrue();
            guardar(autor, 895000L, false);
            assertThat(comunidad.perfil(yo.email(), autor.id(), 0).publicaciones()).isEmpty();
            assertThat(resenas.obtenerResenasPorPelicula(yo.email(), 895000L)).isEmpty();
            assertThat(comunidad.pendientes(admin.email())).isEmpty();
        } finally { admins.remove(admin.id()); }
    }

    @Test
    void suspenderOcultaTodaLaParticipacionSinBorrarBiblioteca() {
        Cuenta yo = crear(true), autor = crear(true), admin = crear(true);
        guardar(autor, 896000L, false);
        comunidad.seguir(yo.email(), autor.id(), true);
        comunidad.reportar(yo.email(), new Reportar(autor.id(), null, Motivo.SPAM));
        @SuppressWarnings("unchecked") Set<Long> admins = (Set<Long>) ReflectionTestUtils.getField(comunidad, "moderadores");
        admins.add(admin.id());
        try {
            comunidad.resolver(admin.email(), comunidad.pendientes(admin.email()).getFirst().id(), Accion.SUSPENDER);
            assertThat(comunidad.buscar(yo.email(), autor.usuario().getUsername())).isEmpty();
            assertThat(comunidad.feed(yo.email(), 0).publicaciones()).isEmpty();
            assertThat(resenas.obtenerMisResenas(autor.email())).hasSize(1);
            assertThat(comunidad.estado(autor.email()).suspendida()).isTrue();
        } finally { admins.remove(admin.id()); }
    }

    @Test
    void borrarResenaYCuentaPurgaReportesYRelacionesSinBorrarOtrasCuentas() {
        Cuenta yo = crear(true), autor = crear(true);
        var resena = guardar(autor, 897000L, false);
        comunidad.reportar(yo.email(), new Reportar(autor.id(), resena.id(), Motivo.OTRO));
        resenas.marcarComoNoVista(autor.email(), 897000L);
        assertThat(reportes.findAll()).isEmpty();
        comunidad.reportar(yo.email(), new Reportar(autor.id(), null, Motivo.OTRO));
        comunidad.seguir(yo.email(), autor.id(), true);
        comunidad.bloquear(autor.email(), yo.id(), true);
        eliminarCuenta.eliminar(autor.email(), "password-inicial");
        assertThat(reportes.findAll()).isEmpty();
        assertThat(relaciones.findAll()).isEmpty();
        assertThat(usuarios.findById(yo.id())).isPresent();
    }

    @Test
    void endpointsExigenSesionYRechazanReporteSinMotivo() throws Exception {
        Cuenta yo = crear(true), autor = crear(true);
        mvc.perform(get("/api/comunidad/estado")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/comunidad/reportes").header("Authorization", "Bearer " + yo.token())
                .contentType("application/json").content("{\"usuarioId\":" + autor.id() + "}"))
                .andExpect(status().isBadRequest());
    }

    private Cuenta crear(boolean participa) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var registro = new RegistroRequest();
        registro.setUsername("cine" + suffix); registro.setEmail("correo" + suffix + "@example.com"); registro.setPassword("password-inicial");
        var auth = cuentas.registrar(registro);
        var usuario = usuarios.findByEmail(registro.getEmail()).orElseThrow();
        if (participa) comunidad.aceptar(usuario.getEmail(), ComunidadService.VERSION_NORMAS);
        return new Cuenta(usuario, auth.getToken());
    }
    private ResenaResponse guardar(Cuenta cuenta, Long tmdbId, boolean spoiler) {
        var request = new ResenaRequest(); request.setTmdbId(tmdbId); request.setTitulo("Película de prueba");
        request.setCalificacion(4); request.setComentario("Una opinión"); request.setSpoiler(spoiler);
        return resenas.guardarResena(cuenta.email(), request);
    }
}
