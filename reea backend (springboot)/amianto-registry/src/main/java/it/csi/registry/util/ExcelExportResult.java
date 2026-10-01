package it.csi.registry.util;

public class ExcelExportResult {
    private final byte[] content;
    private final String fileName;
    private final Integer fileId;
    private final Integer elaborazioneId;

    public ExcelExportResult(byte[] content, String fileName, Integer fileId, Integer elaborazioneId) {
        this.content = content;
        this.fileName = fileName;
        this.fileId = fileId;
        this.elaborazioneId = elaborazioneId;
    }

    public byte[] getContent() { 
    	return content; 
    }
    
    public String getFileName() { 
    	return fileName; 
    }

	public Integer getFileId() {
		return fileId;
	}

	public Integer getElaborazioneId() {
		return elaborazioneId;
	}
    
	
}