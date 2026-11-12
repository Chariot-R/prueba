package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.RegistroAcceso;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    List<RegistroAcceso> findTop20ByOrderByFechaDescIdDesc();

    long countByExitoFalseAndFechaAfter(LocalDateTime desde);
}
