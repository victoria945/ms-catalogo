package org.gestionpastelera.ms_catalogo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.gestionpastelera.ms_catalogo.enums.Categoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PastelResponse {

        private Long id;

        private String codigoPastel;

        private String nombre;

        private String descripcion;

        private Categoria categoria;

        private BigDecimal precioCosto;

        private BigDecimal precioVenta;

        private Integer pesoGramos;

        private Integer diasVencimiento;

        private Boolean activo;

        private Integer stockDisponible;

        private String estadoStock;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime fechaCreacion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime fechaActualizacion;
}
