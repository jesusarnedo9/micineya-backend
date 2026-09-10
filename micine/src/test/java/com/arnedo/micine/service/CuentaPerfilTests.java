package com.arnedo.micine.service;

import com.arnedo.micine.dto.AuthResponse;
import com.arnedo.micine.dto.LoginRequest;
import com.arnedo.micine.dto.RegistroRequest;
import com.arnedo.micine.entity.Pelicula;
import com.arnedo.micine.entity.Resena;
import com.arnedo.micine.repository.*;
import com.arnedo.micine.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaPerfilTests {
    private static final String PASSWORD = "password-inicial";
    @Autowired private UsuarioService usuarios;
    @Autowired private CuentaService cuentas;
    @Autowired private FotoPerfilService fotos;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ResenaRepository resenas;
    @Autowired private PeliculaRepository peliculas;
    @Autowired private GeneroRepository generos;
    @Autowired private PlataformaRepository plataformas;
    @Autowired private JwtService jwt;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    @Test
    void endpointsDeCuentaRequierenSesionYLaPaginaExternaEsPublica() throws Exception {
        mvc.perform(get("/api/users/me/foto")).andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/users/me").contentType("application/json")
                .content("{\"passwordActual\":\"irrelevante\"}")).andExpect(status().is4xxClientError());
        mvc.perform(get("/eliminar-cuenta.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar tu cuenta")));
    }

    @Test
    void eliminarConPasswordIncorrectaNoModificaLaCuenta() throws Exception {
        String email = registrar();
        String token = login(email, PASSWORD).getToken();
        mvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"passwordActual\":\"equivocada\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarioRepository.findByEmail(email)).isPresent();
    }

    @Test
    void cambiarPasswordInvalidaLasSesionesYPermiteLaNuevaClave() throws Exception {
        String email = registrar();
        AuthResponse anterior = login(email, PASSWORD);
        mvc.perform(put("/api/users/me/password").header("Authorization", "Bearer " + anterior.getToken())
                .contentType("application/json")
                .content("{\"passwordActual\":\"password-inicial\",\"passwordNueva\":\"otra-clave-segura\"}"))
                .andExpect(status().isNoContent());
        assertThat(login(email, "otra-clave-segura").getToken()).isNotBlank();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + anterior.getToken()))
                .andExpect(status().is4xxClientError());
        assertThatThrownBy(() -> usuarios.refrescarSesion(anterior.getRefreshToken())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> login(email, PASSWORD)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void borrarCuentaEliminaSusDatosYNoAfectaAOtrosNiReutilizaTokens() throws Exception {
        String email = registrar();
        String otroEmail = registrar();
        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        var otro = usuarioRepository.findByEmail(otroEmail).orElseThrow();
        Long id = usuario.getId();
        AuthResponse anterior = login(email, PASSWORD);
        Pelicula pelicula = peliculas.save(new Pelicula(9012345L, "Película compartida", null));
        usuario.getPeliculasFavoritas().add(pelicula);
        usuario.getGeneros().add(generos.findAll().getFirst());
        usuario.getPlataformas().add(plataformas.findAll().getFirst());
        usuario.getRecomendacionesRecientes().put(pelicula.getTmdbId(), LocalDateTime.now());
        usuario.getPeliculasDescartadas().put(pelicula.getTmdbId(), LocalDateTime.now().plusDays(30));
        otro.getPeliculasFavoritas().add(pelicula);
        resenas.save(new Resena(3, "Reseña propia", usuario, pelicula));
        resenas.save(new Resena(4, "Reseña ajena", otro, pelicula));
        fotos.guardar(email, imagen(300, 200));
        em.flush();

        mvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + anterior.getToken())
                .contentType("application/json").content("{\"passwordActual\":\"password-inicial\"}"))
                .andExpect(status().isNoContent());
        em.clear();
        assertThat(usuarioRepository.findByEmail(email)).isEmpty();
        assertThat(peliculas.findByTmdbId(pelicula.getTmdbId())).isPresent();
        assertThat(resenas.findByUsuarioEmail(otroEmail)).hasSize(1);
        assertThat(usuarioRepository.findByEmail(otroEmail).orElseThrow().getPeliculasFavoritas()).hasSize(1);
        for (String table : List.of("resenas", "usuario_foto_perfil", "usuario_plataforma", "usuario_genero",
                "usuario_pelicula_favorita", "usuario_recomendacion_reciente", "usuario_pelicula_descartada")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where usuario_id = ?", Long.class, id)).isZero();
        }
        registrar(email);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + anterior.getToken()))
                .andExpect(status().is4xxClientError());
        assertThatThrownBy(() -> usuarios.refrescarSesion(anterior.getRefreshToken())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void fotoSeReducePersisteYSePuedeQuitarSinModificarOtraCuenta() throws Exception {
        String email = registrar();
        String otro = registrar();
        fotos.guardar(email, imagen(400, 300));
        em.flush();
        em.clear();
        String uri = fotos.obtener(email).dataUri();
        assertThat(uri).startsWith("data:image/jpeg;base64,");
        byte[] bytes = Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1));
        var imagen = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(imagen.getWidth()).isEqualTo(256);
        assertThat(imagen.getHeight()).isEqualTo(256);
        assertThat(bytes.length).isLessThanOrEqualTo(64 * 1024);
        assertThat(fotos.obtener(otro).dataUri()).isNull();
        fotos.quitar(email);
        em.flush();
        em.clear();
        assertThat(fotos.obtener(email).dataUri()).isNull();
    }

    @Test
    void rechazaFotosInvalidasGrandesOConDimensionesExcesivas() throws Exception {
        String email = registrar();
        String enorme = Base64.getEncoder().encodeToString(new byte[65537]);
        String dimensionesExcesivas = imagen(513, 50);
        for (String invalida : List.of("no-es-base64", enorme, dimensionesExcesivas)) {
            assertThatThrownBy(() -> fotos.guardar(email, invalida)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(fotos.obtener(email).dataUri()).isNull();
    }

    @Test
    void compatibilidadLegacySoloSePermiteEnCuentasAnteriores() {
        String email = registrar();
        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        String token = Jwts.builder().subject(email).claim("ver", 0).claim("typ", "access")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("micine-test-secret-with-at-least-32-characters".getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertThat(jwt.correspondeAUsuario(token, usuario)).isFalse();
        ReflectionTestUtils.setField(usuario, "requiereTokenConId", null);
        assertThat(jwt.correspondeAUsuario(token, usuario)).isTrue();
    }

    private String registrar() { return registrar(UUID.randomUUID() + "@example.com"); }

    private String registrar(String email) {
        var request = new RegistroRequest();
        request.setEmail(email);
        request.setUsername("cine_" + UUID.randomUUID().toString().substring(0, 8));
        request.setPassword(PASSWORD);
        usuarios.registrar(request);
        return email;
    }

    private AuthResponse login(String email, String password) {
        var request = new LoginRequest();
        request.setIdentifier(email);
        request.setPassword(password);
        return usuarios.login(request);
    }

    private String imagen(int width, int height) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpeg", bytes);
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
