package org.gestionpastelera.ms_catalogo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gestionpastelera.ms_catalogo.dto.request.PastelRequest;
import org.gestionpastelera.ms_catalogo.dto.response.PastelResponse;
import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.gestionpastelera.ms_catalogo.service.PastelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@RequiredArgsConstructor
public class PastelController {

    private final PastelService pastelService;

        @GetMapping
    public ResponseEntity<List<PastelResponse>> listarTodos(
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) Boolean activo
    ) {
        return ResponseEntity.ok(pastelService.listarTodos(categoria, activo));
    }

        @GetMapping("/{id}")
    public ResponseEntity<PastelResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pastelService.obtenerPorId(id));
    }

        @GetMapping("/codigo/{codigoPastel}")
    public ResponseEntity<PastelResponse> obtenerPorCodigo(@PathVariable String codigoPastel) {
        return ResponseEntity.ok(pastelService.obtenerPorCodigo(codigoPastel));
    }

        @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR_SUPERVISOR')")
    public ResponseEntity<PastelResponse> crearPastel(@Valid @RequestBody PastelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pastelService.crearPastel(request));
    }

        @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR_SUPERVISOR')")
    public ResponseEntity<PastelResponse> actualizarPastel(
            @PathVariable Long id,
            @Valid @RequestBody PastelRequest request
    ) {
        return ResponseEntity.ok(pastelService.actualizarPastel(id, request));
    }

        @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR_SUPERVISOR')")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo
    ) {
        pastelService.cambiarEstado(id, activo);
        return ResponseEntity.noContent().build();
    }

        @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPastel(@PathVariable Long id) {
        pastelService.eliminarPastel(id);
        return ResponseEntity.noContent().build();
    }
}
