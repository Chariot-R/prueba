package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.Rol;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findAllByOrderByIdAsc();
}
