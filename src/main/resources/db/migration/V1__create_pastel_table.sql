CREATE TABLE IF NOT EXISTS pastel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_pastel VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    categoria VARCHAR(30) NOT NULL,
    precio_costo DECIMAL(12,2) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    peso_gramos INT,
    dias_vencimiento INT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME
);

CREATE INDEX idx_pastel_categoria ON pastel (categoria);
