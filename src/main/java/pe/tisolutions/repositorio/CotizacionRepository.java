package pe.tisolutions.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tisolutions.dominio.Cotizacion;
import pe.tisolutions.dominio.EstadoCotizacion;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    /** Carga el maestro con todas sus líneas en una sola consulta. */
    @Query("select distinct c from Cotizacion c left join fetch c.detalles where c.id = :id")
    Optional<Cotizacion> buscarConDetalles(@Param("id") Integer id);

    @EntityGraph(attributePaths = "cliente")
    @Query("""
            select c from Cotizacion c
            where (:estado is null or c.estado = :estado)
              and (:texto is null
                   or lower(c.codigo) like lower(concat('%', :texto, '%'))
                   or lower(c.cliente.nombreRazonSocial) like lower(concat('%', :texto, '%'))
                   or c.cliente.documento like concat('%', :texto, '%'))
              and (:desde is null or c.fecha >= :desde)
              and (:hasta is null or c.fecha < :hasta)
              and (:incluirCerradas = true
                   or c.estado not in (pe.tisolutions.dominio.EstadoCotizacion.ANULADA,
                                       pe.tisolutions.dominio.EstadoCotizacion.REEMPLAZADA))
            """)
    Page<Cotizacion> buscar(@Param("estado") EstadoCotizacion estado,
                            @Param("texto") String texto,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            @Param("incluirCerradas") boolean incluirCerradas,
                            Pageable pageable);

    @Query(value = "SELECT NEXT VALUE FOR seq_cotizacion", nativeQuery = true)
    Integer siguienteNumero();

    @Query("select max(c.numeroVersion) from Cotizacion c where c.codigo = :codigo")
    Integer ultimaVersion(@Param("codigo") String codigo);

    boolean existsByCotizacionOrigenIdAndEstado(Integer origenId, EstadoCotizacion estado);
}
