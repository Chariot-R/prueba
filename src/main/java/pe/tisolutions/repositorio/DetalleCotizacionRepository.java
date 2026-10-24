package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.DetalleCotizacion;

public interface DetalleCotizacionRepository extends JpaRepository<DetalleCotizacion, Integer> {

    boolean existsByProductoId(Integer productoId);
}
