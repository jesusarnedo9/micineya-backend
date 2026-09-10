package com.arnedo.micine.controller;

import com.arnedo.micine.dto.*;
import com.arnedo.micine.service.BibliotecaService;
import com.arnedo.micine.service.ProgresoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/biblioteca")
public class BibliotecaController {
    private final BibliotecaService biblioteca;
    private final ProgresoService progreso;
    public BibliotecaController(BibliotecaService biblioteca, ProgresoService progreso) {
        this.biblioteca = biblioteca; this.progreso = progreso;
    }
    @GetMapping("/favoritas")
    public List<BibliotecaDtos.Favorita> favoritas(Principal p) { return biblioteca.favoritas(p.getName()); }
    @PostMapping("/favoritas")
    public BibliotecaDtos.Favorita guardar(Principal p, @Valid @RequestBody BibliotecaDtos.Guardar r) {
        return biblioteca.guardar(p.getName(), r);
    }
    @DeleteMapping("/favoritas/{tipo}/{id}")
    public ResponseEntity<Void> quitar(Principal p, @PathVariable String tipo, @PathVariable Long id) {
        biblioteca.quitarGuardada(p.getName(), TipoContenido.desdeCodigo(tipo), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/resenas")
    public List<ResenaResponse> resenas(Principal p) { return biblioteca.mias(p.getName()); }
    @PostMapping("/resenas")
    public ResenaResponse resenar(Principal p, @Valid @RequestBody BibliotecaDtos.Resenar r) {
        return biblioteca.resenar(p.getName(), r);
    }
    @DeleteMapping("/resenas/{tipo}/{id}")
    public ResponseEntity<Void> noVista(Principal p, @PathVariable String tipo, @PathVariable Long id) {
        biblioteca.marcarNoVista(p.getName(), TipoContenido.desdeCodigo(tipo), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/progreso")
    public ProgresoResponse progreso(Principal p) { return progreso.propio(p.getName()); }
}
