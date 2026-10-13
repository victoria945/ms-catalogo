package org.gestionpastelera.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;
import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pastel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pastel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_pastel", nullable = false, unique = true, length = 30)
    private String codigoPastel;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "categoria", nullable = false, length = 30)
    private Categoria categoria;

    @Column(name = "precio_costo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCosto;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "peso_gramos")
    private Integer pesoGramos;

    @Column(name = "dias_vencimiento")
    private Integer diasVencimiento;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
