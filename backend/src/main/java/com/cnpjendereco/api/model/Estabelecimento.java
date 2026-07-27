package com.cnpjendereco.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estabelecimentos", indexes = {
    @Index(name = "idx_uf_mun", columnList = "uf, municipio"),
    @Index(name = "idx_uf_log", columnList = "uf, logradouro"),
    @Index(name = "idx_cep", columnList = "cep")
})
public class Estabelecimento {

    @Id
    @Column(name = "cnpj", length = 14, nullable = false)
    private String cnpj;            // 14 dígitos, sem máscara

    @Column(name = "uf", length = 2, nullable = false)
    private String uf;

    @Column(name = "municipio", length = 7, nullable = false)
    private String municipio;        // código IBGE do município (7 dígitos)

    @Column(name = "nome_municipio", length = 120)
    private String nomeMunicipio;   // preenchido no ETL via tabela de municípios

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "tipo_logradouro", length = 40)
    private String tipoLogradouro;

    @Column(name = "logradouro", length = 200)
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "complemento", length = 200)
    private String complemento;

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(name = "situacao_cadastral", length = 2)
    private String situacaoCadastral;

    @Column(name = "cnae_principal", length = 7)
    private String cnaePrincipal;

    public Estabelecimento() {}

    // getters e setters
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getNomeMunicipio() { return nomeMunicipio; }
    public void setNomeMunicipio(String nomeMunicipio) { this.nomeMunicipio = nomeMunicipio; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getTipoLogradouro() { return tipoLogradouro; }
    public void setTipoLogradouro(String tipoLogradouro) { this.tipoLogradouro = tipoLogradouro; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public String getSituacaoCadastral() { return situacaoCadastral; }
    public void setSituacaoCadastral(String situacaoCadastral) { this.situacaoCadastral = situacaoCadastral; }
    public String getCnaePrincipal() { return cnaePrincipal; }
    public void setCnaePrincipal(String cnaePrincipal) { this.cnaePrincipal = cnaePrincipal; }
}
