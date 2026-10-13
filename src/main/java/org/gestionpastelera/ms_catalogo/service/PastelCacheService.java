package org.gestionpastelera.ms_catalogo.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import org.gestionpastelera.ms_catalogo.dto.response.PastelResponse;
import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.gestionpastelera.ms_catalogo.mapper.PastelMapper;
import org.gestionpastelera.ms_catalogo.model.Pastel;
import org.gestionpastelera.ms_catalogo.repository.PastelRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PastelCacheService {

    private final PastelRepository pastelRepository;
    private final PastelMapper mapper;

    @Transactional(readOnly = true)
    @Cacheable("catalogoCache")
    public List<PastelResponse> listarTodosBase(Categoria categoria, Boolean activo) {
        List<Pastel> lista;
        if (categoria != null && activo != null) {
            lista = pastelRepository.findByCategoriaAndActivo(categoria, activo);
        } else if (categoria != null) {
            lista = pastelRepository.findByCategoria(categoria);
        } else if (activo != null) {
            lista = pastelRepository.findByActivo(activo);
        } else {
            lista = pastelRepository.findAll();
        }
        return mapper.toResponseList(lista);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "catalogoCache", key = "'id_' + #id")
    public PastelResponse obtenerPorIdBase(Long id) {
        Pastel pastel = pastelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con ID: " + id));
        return mapper.toResponse(pastel);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "catalogoCache", key = "'cod_' + #codigoPastel")
    public PastelResponse obtenerPorCodigoBase(String codigoPastel) {
        Pastel pastel = pastelRepository.findByCodigoPastel(codigoPastel)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pastel no encontrado con código: " + codigoPastel));
        return mapper.toResponse(pastel);
    }

    @CacheEvict(value = "catalogoCache", allEntries = true)
    public void invalidarCache() {
        // Método para invalidar caché al realizar mutaciones
    }
}
