package org.gestionpastelera.ms_catalogo.mapper;

import org.gestionpastelera.ms_catalogo.dto.request.PastelRequest;
import org.gestionpastelera.ms_catalogo.dto.response.PastelResponse;
import org.gestionpastelera.ms_catalogo.model.Pastel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PastelMapper {

    @Mapping(target = "stockDisponible", ignore = true)
    @Mapping(target = "estadoStock", ignore = true)
    PastelResponse toResponse(Pastel pastel);

    List<PastelResponse> toResponseList(List<Pastel> pasteles);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "fechaCreacion", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "fechaActualizacion", expression = "java(java.time.LocalDateTime.now())")
    Pastel toEntity(PastelRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", expression = "java(java.time.LocalDateTime.now())")
    void updateEntityFromRequest(PastelRequest request, @MappingTarget Pastel pastel);
}
