package org.gestionpastelera.ms_catalogo.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestionpastelera.ms_catalogo.client.InventarioClient;
import org.gestionpastelera.ms_catalogo.dto.request.PastelRequest;
import org.gestionpastelera.ms_catalogo.dto.response.PastelResponse;
import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.gestionpastelera.ms_catalogo.mapper.PastelMapper;
import org.gestionpastelera.ms_catalogo.model.Pastel;
import org.gestionpastelera.ms_catalogo.repository.PastelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public PastelResponse crearPastel(PastelRequest request) {
        if (pastelRepository.existsByCodigoPastel(request.getCodigoPastel().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un pastel con el código: " + request.getCodigoPastel());
        }

        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de venta no puede ser inferior al precio de costo");
        }

        Pastel pastel = mapper.toEntity(request);
        Pastel guardado = pastelRepository.save(pastel);
        cacheService.invalidarCache();

        PastelResponse res = mapper.toResponse(guardado);
        Map<String, Integer> stockMap = consultarStockSeguro(List.of(res.getCodigoPastel()));
        return enriquecerConStock(res, stockMap);
    }

    @Transactional
    public PastelResponse actualizarPastel(Long id, PastelRequest request) {
        Pastel pastel = pastelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con ID: " + id));

        if (!pastel.getCodigoPastel().equalsIgnoreCase(request.getCodigoPastel().trim())
                && pastelRepository.existsByCodigoPastel(request.getCodigoPastel().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro pastel con el código: " + request.getCodigoPastel());
        }

        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de venta no puede ser inferior al precio de costo");
        }

        mapper.updateEntityFromRequest(request, pastel);
        Pastel actualizado = pastelRepository.save(pastel);
        cacheService.invalidarCache();

        PastelResponse res = mapper.toResponse(actualizado);
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

    private Map<String, Integer> consultarStockSeguro(List<String> codigos) {
        try {
            return inventarioClient.obtenerStockBulk(codigos);
        } catch (Exception e) {
            log.warn("No fue posible comunicarse con ms-inventario para consultar stock: {}", e.getMessage());
            return null;
        }
    }

    private PastelResponse enriquecerConStock(PastelResponse base, Map<String, Integer> stockMap) {
        if (stockMap == null) {
            return base.toBuilder()
                    .stockDisponible(null)
                    .estadoStock("NO_DISPONIBLE")
                    .build();
        }

        Integer stock = stockMap.get(base.getCodigoPastel());
        int stockFinal = stock != null ? stock : 0;
        String estado = stockFinal > 0 ? "DISPONIBLE" : "SIN_STOCK";

        return base.toBuilder()
                .stockDisponible(stockFinal)
                .estadoStock(estado)
                .build();
    }
}
