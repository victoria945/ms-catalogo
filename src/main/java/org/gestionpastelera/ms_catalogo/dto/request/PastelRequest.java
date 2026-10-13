package org.gestionpastelera.ms_catalogo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.gestionpastelera.ms_catalogo.enums.Categoria;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastelRequest {

    @NotBlank(message = "El código del pastel es obligatorio")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres")
        private String codigoPastel;

    @NotBlank(message = "El nombre del pastel es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
        private Categoria categoria;

    @NotNull(message = "El precio de costo es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de costo no puede ser negativo")
        private BigDecimal precioCosto;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        private BigDecimal precioVenta;

        private Integer pesoGramos;

        private Integer diasVencimiento;
}
