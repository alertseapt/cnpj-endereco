package com.cnpjendereco.api.dto;

public class BuscaRequest {
    private String uf;                 // obrigatório
    private String municipio;          // obrigatório (código IBGE 7 dígitos)
    private String logradouro;         // opcional
    private String numero;             // opcional
    private String bairro;             // opcional
    private String cep;                // opcional

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
}
