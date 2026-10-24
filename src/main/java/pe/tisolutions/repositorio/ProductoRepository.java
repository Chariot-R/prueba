package pe.tisolutions.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tisolutions.dominio.Producto;

import java.util.Collection;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    @Query("""
            select p from Producto p join fetch p.proveedor pr
            where p.activo = true
              and (lower(p.nombre) like lower(concat('%', :q, '%'))
                   or lower(pr.nombre) like lower(concat('%', :q, '%')))
            order by p.nombre
            """)
    List<Producto> buscarActivos(@Param("q") String q, Pageable pageable);

    @Query("select p from Producto p join fetch p.proveedor where p.id in :ids")
    List<Producto> buscarPorIds(@Param("ids") Collection<Integer> ids);
}
