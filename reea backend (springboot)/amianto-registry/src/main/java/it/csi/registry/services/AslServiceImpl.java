package it.csi.registry.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.csi.registry.model.AslDTO;
import it.csi.registry.repositories.AslRepository;

@Service
public class AslServiceImpl implements AslService {

    private final AslRepository aslRepository;

    public AslServiceImpl(AslRepository aslRepository) {
        this.aslRepository = aslRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AslDTO> getListaAslCompetenza() {
        return aslRepository.findAllAslCompetenza();
    }

	@Override
	public Integer getAslIdByAslCod(String aslCod) {
		return aslRepository.getAslIdByAslCod(aslCod);
	}

    @Override
    @Transactional(readOnly = true)
    public List<AslDTO> getListaFiltroAssistiti() {
        return aslRepository.findListaFiltroAssistiti();
    }

}

