package pe.tisolutions.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tisolutions.dominio.Producto;

import java.util.Collection;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /**
     * Búsqueda sin distinguir mayúsculas NI tildes: "camara" encuentra "Cámara".
     * (La base usa Modern_Spanish_CI_AS, que sí distingue tildes; por eso el COLLATE ..._AI.)
     */
    @Query(value = """
            SELECT p.* FROM producto p JOIN proveedor pr ON pr.id = p.proveedor_id
            WHERE p.activo = 1
              AND (p.nombre COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' COLLATE Latin1_General_CI_AI
                   OR pr.nombre COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' COLLATE Latin1_General_CI_AI)
            ORDER BY p.nombre
            """, nativeQuery = true)
    List<Producto> buscarActivos(@Param("q") String q, Pageable pageable);

    @Query("select p from Producto p join fetch p.proveedor where p.id in :ids")
    List<Producto> buscarPorIds(@Param("ids") Collection<Integer> ids);
}
