package pe.tisolutions.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tisolutions.dominio.EstadoSolicitud;
import pe.tisolutions.dominio.Solicitud;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    List<Solicitud> findByClienteIdAndEstadoNotOrderByFechaCreacionDesc(Integer clienteId, EstadoSolicitud estado);
}
