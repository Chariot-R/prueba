package pe.tisolutions.dominio;

public enum TipoItem {
    PRODUCTO("Producto"),
    MATERIAL("Material"),
    MANO_OBRA("Mano de obra");

    private final String etiqueta;

    TipoItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
