package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.CuentaCliente;

import java.util.Optional;

public interface CuentaClienteRepository extends JpaRepository<CuentaCliente, Integer> {

    Optional<CuentaCliente> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
