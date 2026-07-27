package com.cnpjendereco.api.service;

import com.cnpjendereco.api.dto.BuscaRequest;
import com.cnpjendereco.api.dto.EstabelecimentoResponse;
import com.cnpjendereco.api.model.Estabelecimento;
import com.cnpjendereco.api.repository.EstabelecimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BuscaService {

    private final EstabelecimentoRepository repository;

    public BuscaService(EstabelecimentoRepository repository) {
        this.repository = repository;
    }

    public Page<EstabelecimentoResponse> buscar(BuscaRequest req, Pageable pageable) {
        Specification<Estabelecimento> spec = Specification.where(null);

        // Obrigatórios
        String uf = req.getUf() == null ? "" : req.getUf().toUpperCase();
        spec = spec.and((root, q, cb) -> cb.equal(root.get("uf"), uf));

        // Município: aceita código IBGE (7 dígitos) OU nome (LIKE).
        // Assim o usuário pode digitar "São Paulo" ou "3550308".
        String mun = req.getMunicipio() == null ? "" : req.getMunicipio().trim();
        if (mun.matches("\\d{7}")) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("municipio"), mun));
        } else {
            spec = spec.and((root, q, cb) ->
                cb.like(cb.lower(root.get("nomeMunicipio")), "%" + mun.toLowerCase() + "%"));
        }

        // Opcionais — só adiciona se preenchido (filtro mais fino)
        if (StringUtils.hasText(req.getLogradouro())) {
            spec = spec.and((root, q, cb) ->
                cb.like(cb.lower(root.get("logradouro")), "%" + req.getLogradouro().toLowerCase() + "%"));
        }
        if (StringUtils.hasText(req.getNumero())) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("numero"), req.getNumero()));
        }
        if (StringUtils.hasText(req.getBairro())) {
            spec = spec.and((root, q, cb) ->
                cb.like(cb.lower(root.get("bairro")), "%" + req.getBairro().toLowerCase() + "%"));
        }
        if (StringUtils.hasText(req.getCep())) {
            String cep = req.getCep().replaceAll("\\D", "");
            spec = spec.and((root, q, cb) -> cb.equal(root.get("cep"), cep));
        }

        return repository.findAll(spec, pageable).map(EstabelecimentoResponse::new);
    }
}
