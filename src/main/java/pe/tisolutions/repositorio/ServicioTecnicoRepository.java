package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.ServicioTecnico;

public interface ServicioTecnicoRepository extends JpaRepository<ServicioTecnico, Integer> {

    boolean existsByCotizacionId(Integer cotizacionId);

    boolean existsByCotizacionIdAndEstadoNot(Integer cotizacionId, String estado);
}
