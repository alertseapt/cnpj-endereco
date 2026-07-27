package com.cnpjendereco.api.dto;

import com.cnpjendereco.api.model.Estabelecimento;

public class EstabelecimentoResponse {
    public final String cnpj;
    public final String razaoSocial;
    public final String nomeFantasia;
    public final String uf;
    public final String municipio;
    public final String nomeMunicipio;
    public final String bairro;
    public final String tipoLogradouro;
    public final String logradouro;
    public final String numero;
    public final String complemento;
    public final String cep;
    public final String situacaoCadastral;
    public final String cnaePrincipal;

    public EstabelecimentoResponse(Estabelecimento e) {
        this.cnpj = formatCnpj(e.getCnpj());
        this.razaoSocial = e.getRazaoSocial();
        this.nomeFantasia = e.getNomeFantasia();
        this.uf = e.getUf();
        this.municipio = e.getMunicipio();
        this.nomeMunicipio = e.getNomeMunicipio();
        this.bairro = e.getBairro();
        this.tipoLogradouro = e.getTipoLogradouro();
        this.logradouro = e.getLogradouro();
        this.numero = e.getNumero();
        this.complemento = e.getComplemento();
        this.cep = e.getCep();
        this.situacaoCadastral = e.getSituacaoCadastral();
        this.cnaePrincipal = e.getCnaePrincipal();
    }

    private static String formatCnpj(String c) {
        if (c == null || c.length() != 14) return c;
        return c.substring(0,2) + "." + c.substring(2,5) + "." + c.substring(5,8) +
               "/" + c.substring(8,12) + "-" + c.substring(12,14);
    }
}
