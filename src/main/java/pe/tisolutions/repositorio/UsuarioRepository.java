package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByNombreAsc();

    long countByEstado(String estado);
}
