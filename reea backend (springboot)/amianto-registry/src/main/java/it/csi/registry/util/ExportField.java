package it.csi.registry.util;

public class ExportField {
    private final String nomeCampo;
    private final String chiaveMappa;
    private final String header;

    public ExportField(String nomeCampo, String chiaveMappa, String header) {
        this.nomeCampo = nomeCampo;
        this.chiaveMappa = chiaveMappa;
        this.header = header;
    }

    public String getNomeCampo() {
        return nomeCampo;
    }

    public String getChiaveMappa() {
        return chiaveMappa;
    }

    public String getHeader() {
        return header;
    }
    
    

}
