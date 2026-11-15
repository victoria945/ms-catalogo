package org.gestionpastelera.ms_catalogo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestionpastelera.ms_catalogo.client.InventarioClient;
import org.gestionpastelera.ms_catalogo.dto.request.PastelRequest;
import org.gestionpastelera.ms_catalogo.dto.response.PastelResponse;
import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.gestionpastelera.ms_catalogo.mapper.PastelMapper;
import org.gestionpastelera.ms_catalogo.model.Pastel;
import org.gestionpastelera.ms_catalogo.repository.PastelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PastelService {

    private final PastelRepository pastelRepository;
    private final PastelCacheService cacheService;
    private final PastelMapper mapper;
    private final InventarioClient inventarioClient;
    private final TransactionTemplate transactionTemplate;

    public List<PastelResponse> listarTodos(Categoria categoria, Boolean activo) {
        List<PastelResponse> baseList = cacheService.listarTodosBase(categoria, activo);
        if (baseList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> codigos = baseList.stream()
                .map(PastelResponse::getCodigoPastel)
                .collect(Collectors.toList());

        Map<String, Integer> stockMap = consultarStockSeguro(codigos);

        return baseList.stream()
                .map(p -> enriquecerConStock(p, stockMap))
                .collect(Collectors.toList());
    }

    public PastelResponse obtenerPorId(Long id) {
        PastelResponse base = cacheService.obtenerPorIdBase(id);
        Map<String, Integer> stockMap = consultarStockSeguro(List.of(base.getCodigoPastel()));
        return enriquecerConStock(base, stockMap);
    }

    public PastelResponse obtenerPorCodigo(String codigoPastel) {
        PastelResponse base = cacheService.obtenerPorCodigoBase(codigoPastel);
        Map<String, Integer> stockMap = consultarStockSeguro(List.of(base.getCodigoPastel()));
        return enriquecerConStock(base, stockMap);
    }

    public PastelResponse crearPastel(PastelRequest request) {
        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de venta no puede ser inferior al precio de costo");
        }

        String codigoLimpio = request.getCodigoPastel().trim();

        PastelResponse res = transactionTemplate.execute(status -> {
            if (pastelRepository.existsByCodigoPastel(codigoLimpio)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un pastel con el código: " + codigoLimpio);
            }

            Pastel pastel = mapper.toEntity(request);
            pastel.setCodigoPastel(codigoLimpio);
            pastel.setNombre(request.getNombre().trim());
            Pastel guardado = pastelRepository.save(pastel);
            return mapper.toResponse(guardado);
        });

        cacheService.invalidarCache();

        Map<String, Integer> stockMap = consultarStockSeguro(List.of(res.getCodigoPastel()));
        return enriquecerConStock(res, stockMap);
    }

    public PastelResponse actualizarPastel(Long id, PastelRequest request) {
        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de venta no puede ser inferior al precio de costo");
        }

        String codigoLimpio = request.getCodigoPastel().trim();

        PastelResponse res = transactionTemplate.execute(status -> {
            Pastel pastel = pastelRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con ID: " + id));

            if (!pastel.getCodigoPastel().equalsIgnoreCase(codigoLimpio)
                    && pastelRepository.existsByCodigoPastel(codigoLimpio)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro pastel con el código: " + codigoLimpio);
            }

            mapper.updateEntityFromRequest(request, pastel);
            pastel.setCodigoPastel(codigoLimpio);
            pastel.setNombre(request.getNombre().trim());
            Pastel actualizado = pastelRepository.save(pastel);
            return mapper.toResponse(actualizado);
        });

        cacheService.invalidarCache();

        Map<String, Integer> stockMap = consultarStockSeguro(List.of(res.getCodigoPastel()));
        return enriquecerConStock(res, stockMap);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Pastel pastel = pastelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con ID: " + id));
        pastel.setActivo(activo);
        pastel.setFechaActualizacion(LocalDateTime.now());
        pastelRepository.save(pastel);
        cacheService.invalidarCache();
    }

    @Transactional
    public void eliminarPastel(Long id) {
        Pastel pastel = pastelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con ID: " + id));
        pastelRepository.delete(pastel);
        cacheService.invalidarCache();
    }

    // Consultar stock de forma segura
    private Map<String, Integer> consultarStockSeguro(List<String> codigos) {
        try {
            return inventarioClient.obtenerStockBulk(codigos);
        } catch (Exception e) {
            log.warn("No fue posible comunicarse con ms-inventario para consultar stock: {}", e.getMessage());
            return null;
        }
    }

    // Costo visible solo para autorizados
    private boolean puedeVerPrecioCosto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_VENDEDOR_SUPERVISOR".equals(role));
    }

    private PastelResponse enriquecerConStock(PastelResponse base, Map<String, Integer> stockMap) {
        java.math.BigDecimal costoVisible = puedeVerPrecioCosto() ? base.getPrecioCosto() : null;

        if (stockMap == null) {
            return base.toBuilder()
                    .precioCosto(costoVisible)
                    .stockDisponible(null)
                    .estadoStock("NO_DISPONIBLE")
                    .build();
        }

        Integer stock = stockMap.get(base.getCodigoPastel());
        int stockFinal = stock != null ? stock : 0;
        String estado = stockFinal > 0 ? "DISPONIBLE" : "SIN_STOCK";

        return base.toBuilder()
                .precioCosto(costoVisible)
                .stockDisponible(stockFinal)
                .estadoStock(estado)
                .build();
    }
}
