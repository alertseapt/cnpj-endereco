package com.cnpjendereco.api.repository;

import com.cnpjendereco.api.model.Estabelecimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstabelecimentoRepository
        extends JpaRepository<Estabelecimento, String>, JpaSpecificationExecutor<Estabelecimento> {

    // Busca progressiva: UF + Município obrigatórios; demais parâmetros opcionais.
    // Usa Specification no service para compor os filtros dinamicamente.
    @Query("SELECT DISTINCT e.municipio, e.nomeMunicipio, COUNT(e) " +
           "FROM Estabelecimento e WHERE e.uf = :uf GROUP BY e.municipio, e.nomeMunicipio")
    java.util.List<Object[]> listMunicipiosPorUf(@Param("uf") String uf);

    long deleteByUf(String uf);
}
