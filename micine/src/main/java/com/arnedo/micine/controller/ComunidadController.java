package com.arnedo.micine.controller;

import com.arnedo.micine.dto.ComunidadDtos.*;
import com.arnedo.micine.service.ComunidadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comunidad")
public class ComunidadController {
    private final ComunidadService comunidad;
    public ComunidadController(ComunidadService comunidad) { this.comunidad = comunidad; }
    @GetMapping("/estado") public Estado estado(Principal p) { return comunidad.estado(p.getName()); }
    @PostMapping("/normas") public void aceptar(Principal p, @Valid @RequestBody AceptarNormas request) { comunidad.aceptar(p.getName(), request.version()); }
    @GetMapping("/buscar") public List<Persona> buscar(Principal p, @RequestParam(defaultValue = "") String username) { return comunidad.buscar(p.getName(), username); }
    @GetMapping("/perfiles/{id}") public Perfil perfil(Principal p, @PathVariable Long id, @RequestParam(defaultValue = "0") int pagina) { return comunidad.perfil(p.getName(), id, pagina); }
    @GetMapping("/siguiendo") public Pagina feed(Principal p, @RequestParam(defaultValue = "0") int pagina) { return comunidad.feed(p.getName(), pagina); }
    @PutMapping("/siguiendo/{id}") public void seguir(Principal p, @PathVariable Long id) { comunidad.seguir(p.getName(), id, true); }
    @DeleteMapping("/siguiendo/{id}") public void dejarSeguir(Principal p, @PathVariable Long id) { comunidad.seguir(p.getName(), id, false); }
    @GetMapping("/bloqueados") public List<Persona> bloqueados(Principal p) { return comunidad.misBloqueados(p.getName()); }
    @PutMapping("/bloqueados/{id}") public void bloquear(Principal p, @PathVariable Long id) { comunidad.bloquear(p.getName(), id, true); }
    @DeleteMapping("/bloqueados/{id}") public void desbloquear(Principal p, @PathVariable Long id) { comunidad.bloquear(p.getName(), id, false); }
    @PostMapping("/reportes") public void reportar(Principal p, @Valid @RequestBody Reportar request) { comunidad.reportar(p.getName(), request); }
    @GetMapping("/moderacion") public List<Reporte> moderacion(Principal p) { return comunidad.pendientes(p.getName()); }
    @PutMapping("/moderacion/{id}") public void resolver(Principal p, @PathVariable Long id, @Valid @RequestBody Resolver request) { comunidad.resolver(p.getName(), id, request.accion()); }
}
