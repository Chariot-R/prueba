package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
}
