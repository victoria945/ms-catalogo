package org.gestionpastelera.ms_catalogo.repository;

import org.gestionpastelera.ms_catalogo.enums.Categoria;
import org.gestionpastelera.ms_catalogo.model.Pastel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PastelRepository extends JpaRepository<Pastel, Long> {

    Optional<Pastel> findByCodigoPastel(String codigoPastel);

    boolean existsByCodigoPastel(String codigoPastel);

    List<Pastel> findByCategoria(Categoria categoria);

    List<Pastel> findByActivo(Boolean activo);

    List<Pastel> findByCategoriaAndActivo(Categoria categoria, Boolean activo);
}
