package it.csi.registry.aura.services;

import it.csi.registry.aura.dto.AuraAssistitoDto;
import it.csi.registry.model.AnagraficaDTO;
import it.csi.registry.model.AslDTO;
import it.csi.registry.model.EsenzioneDTO;
import it.csi.registry.repositories.AnagraficaRepository;
import it.csi.registry.repositories.AslRepository;
import it.csi.registry.repositories.EsenzioneRepository;
import it.csi.registry.soap.anagrafe.services.AnagrafeFindService;
import it.csi.registry.soap.anagrafe.services.AnagrafeGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AuraServiceImpl implements AuraService {

    @Autowired  // o via costruttore
    private AnagraficaRepository anagraficaRepository;

    @Autowired
    private EsenzioneRepository esenzioneRepository;

    private final AnagrafeGetService anagrafeGetService;
    private final AnagrafeFindService anagrafeFindService;
    private final AslRepository aslRepository;

    public AuraServiceImpl(AnagrafeGetService anagrafeGetService,
                           AnagrafeFindService anagrafeFindService,
                           AslRepository aslRepository) {
        this.anagrafeGetService = anagrafeGetService;
        this.anagrafeFindService = anagrafeFindService;
        this.aslRepository = aslRepository;
    }


    @Override
    public AuraAssistitoDto getByIdAura(String idAura) {
        AuraAssistitoDto dto = anagrafeGetService.getByIdAura(idAura);
        if (dto == null) return null;  // ← uscita anticipata

        if (dto.getAslAssistenzaCod() != null) {
            AslDTO asl = aslRepository.findByCod(dto.getAslAssistenzaCod());
            if (asl != null) dto.setAslAssistenzaDesc(asl.getAslAziendaDesc());
        }

        // Arricchisci esenzioni AURA con descrizioni da reea_d_esenzione
        if (dto.getEsenzioni() != null) {
            dto.getEsenzioni().forEach(e -> {
                if (e.getEsenzioneCod() != null) {
                    EsenzioneDTO desc = esenzioneRepository.findDescrizioniByCods(
                            e.getEsenzioneCod(), e.getDiagnosiCod());
                    if (desc != null) {
                        if (e.getEsenzioneDesc() == null) e.setEsenzioneDesc(desc.getEsenzioneDesc());
                        e.setDiagnosiDesc(desc.getDiagnosiDesc());
                    }
                }
            });
        }

        // Esenzioni da REEA DB se soggetto già presente
        if (dto.getCodiceFiscale() != null) {
            try {
                AnagraficaDTO soggetto = anagraficaRepository.findByCodiceFiscale(dto.getCodiceFiscale());
                if (soggetto != null && soggetto.getSoggettoId() != null) {
                    dto.setEsenzioni(
                            esenzioneRepository.findListaEsenzioniByIdSoggetto(soggetto.getSoggettoId())
                    );
                }
            } catch (Exception ignored) {}
        }

        return dto;
    }



    @Override
    public AuraAssistitoDto findByCodiceFiscale(String codiceFiscale) {
        AuraAssistitoDto basic = anagrafeFindService.findByCodiceFiscale(codiceFiscale);
        if (basic == null) return null;

        // Il FIND non restituisce data_decesso/data_fine_asl → carica il record completo
        if (basic.getIdAura() != null) {
            AuraAssistitoDto full = getByIdAura(basic.getIdAura());
            return full != null ? full : basic;
        }
        return basic;
    }



    @Override
    public List<AuraAssistitoDto> findByAnagrafica(String cognome, String nome, String dataNascita) {
        List<AuraAssistitoDto> results = anagrafeFindService.findByAnagrafica(cognome, nome, dataNascita);
        if (results == null) return List.of();
        return results;
    }

}