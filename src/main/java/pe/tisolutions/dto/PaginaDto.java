package pe.tisolutions.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Página de resultados en un formato simple para el frontend. */
public record PaginaDto<T>(List<T> contenido, int pagina, int totalPaginas, long totalElementos) {

    public static <T> PaginaDto<T> de(Page<T> p) {
        return new PaginaDto<>(p.getContent(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }
}
