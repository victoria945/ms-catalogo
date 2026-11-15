# 🏷️ Microservicio de Catálogo de Pasteles (`ms-catalogo`)

Microservicio autónomo para la administración de pasteles, precios, categorías y consulta de stock en tiempo real mediante OpenFeign hacia `ms-inventario`.

- **Puerto:** `8081`
- **Base de Datos:** MySQL (`catalogodb`) / H2 en memoria para pruebas
- **Seguridad:** Validación descentralizada de token JWT emitido por `ms-auth`
- **Caché:** Spring Cache (`catalogoCache`) para optimización de consultas

## Endpoints Principales
- `GET /api/v1/catalogo`: Listar pasteles con stock y filtros
- `GET /api/v1/catalogo/{id}`: Detalle de pastel con stock en tiempo real
- `GET /api/v1/catalogo/codigo/{codigoPastel}`: Detalle por código con stock
- `POST /api/v1/catalogo`: Crear pastel (Permiso GESTION)
- `PUT /api/v1/catalogo/{id}`: Actualizar pastel (Permiso GESTION)
- `PATCH /api/v1/catalogo/{id}/estado?activo=true|false`: Activar/Desactivar publicación
- `DELETE /api/v1/catalogo/{id}`: Eliminar pastel (Permiso ADMIN)

## Ejecución Local
```bash
mvn spring-boot:run
```

## Ejecución con Docker
```bash
docker network create pasteleria-network # Solo si no ha sido creada
docker compose up --build
```
