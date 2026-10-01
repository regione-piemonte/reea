package it.csi.registry.services;

import java.io.File;
import java.util.List;
import org.springframework.stereotype.Service;
import it.csi.registry.model.PdfInfo;
import it.csi.registry.model.PdfListResponse;
import it.csi.registry.repositories.PdfRepository;

@Service
public class PdfServiceImpl implements PdfService {

    private final PdfRepository pdfRepository;

    // Costruttore manuale
    public PdfServiceImpl(PdfRepository pdfRepository) {
        this.pdfRepository = pdfRepository;
    }
    
    
    @Override
    public PdfListResponse listaPdfPerPrefisso(int prefisso) {
        if (prefisso <= 0) {
            throw new IllegalArgumentException("Il prefisso deve essere un numero positivo");
        }

        List<PdfInfo> pdfList = pdfRepository.cercaPerPrefisso(prefisso);

        PdfListResponse response = new PdfListResponse();
        response.setPrefisso(prefisso);
        response.setTotaleTrovati(pdfList.size());
        response.setPdfList(pdfList);

        return response;
    }
    
    
    public File getFile(String nomeFile) {
        return pdfRepository.getFileSync(nomeFile);
    }
}
