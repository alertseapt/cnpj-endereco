package com.cnpjendereco.api.service;

import com.cnpjendereco.api.model.Estabelecimento;
import com.cnpjendereco.api.repository.EstabelecimentoRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ETL da Receita Federal (dados abertos, gratuitos).
 * Fonte: https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9
 * Baixa ESTABELECIMENTO_<UF>.zip, descompacta e importa os estabelecimentos.
 */
@Service
public class EtlService {

    private final EstabelecimentoRepository repository;
    private final String dataDir;

    // Espelho com CDN (mais rápido). Mantém o oficial como fallback.
    private static final String BASE_URL =
        "https://arquivos.receitafederal.gov.br/CNPJ/dados_abertos_cnpj/";

    public EtlService(EstabelecimentoRepository repository,
                      @Value("${cnpj.data.dir:/data}") String dataDir) {
        this.repository = repository;
        this.dataDir = dataDir;
    }

    @PostConstruct
    void init() {
        new File(dataDir).mkdirs();
    }

    public record EtlResult(long importados, String arquivo, String erro) {}

    public EtlResult importarUf(String uf) {
        uf = uf.toUpperCase();
        String zipName = "ESTABELECIMENTO_" + uf + ".zip";
        File zipFile = new File(dataDir, zipName);
        try {
            // 1) Baixar (confirma tamanho > 0 — grounded-action)
            downloadIfMissing(zipFile, BASE_URL + zipName);
            if (!zipFile.exists() || zipFile.length() == 0) {
                return new EtlResult(0, zipName, "download falhou ou arquivo vazio");
            }

            // 2) Descompactar e localizar o .csv de estabelecimentos
            File csv = unzipEstabelecimentoCsv(zipFile);

            // 2b) Tabela de MUNICÍPIOS (código IBGE -> nome), 1 download nacional pequeno
            java.util.Map<String, String> munMap = carregarMunicipios();

            // 3) Importar (DELETE + INSERT por UF para reimportação limpa)
            repository.deleteByUf(uf);
            long count = importCsv(csv, uf, munMap);

            return new EtlResult(count, zipName, null);
        } catch (Exception e) {
            return new EtlResult(0, zipName, e.getMessage());
        }
    }

    private void downloadIfMissing(File target, String urlStr) throws IOException {
        if (target.exists() && target.length() > 0) return;
        URL url = new URL(urlStr);
        FileUtils.copyURLToFile(url, target, 30000, 600000); // timeout 10min
    }

    private File unzipEstabelecimentoCsv(File zipFile) throws IOException {
        File outDir = new File(dataDir, "unzip");
        outDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toUpperCase();
                if (name.contains("ESTABELECIMENTO") && name.endsWith(".CSV")) {
                    File out = new File(outDir, entry.getName());
                    Files.copy(zis, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return out;
                }
                zis.closeEntry();
            }
        }
        throw new IOException("CSV de estabelecimentos não encontrado no zip " + zipFile.getName());
    }

    private java.util.Map<String, String> carregarMunicipios() throws IOException {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        File zip = new File(dataDir, "MUNICIPIOS.zip");
        downloadIfMissing(zip, BASE_URL + "MUNICIPIOS.zip");
        if (!zip.exists() || zip.length() == 0) return map;
        File csv = null;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toUpperCase().contains("MUNICIPIO") && entry.getName().endsWith(".CSV")) {
                    csv = new File(dataDir, "unzip/MUNICIPIOS.csv");
                    Files.copy(zis, csv.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                zis.closeEntry();
            }
        }
        if (csv == null) return map;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(csv.toPath()), StandardCharsets.ISO_8859_1))) {
            br.readLine(); // header
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] f = linha.split(";", -1);
                // Layout MUNICÍPIOS: codigo;nome;... (2 colunas principais)
                if (f.length < 2) continue;
                map.put(f[0].trim(), f[1].trim());
            }
        }
        return map;
    }

    private long importCsv(File csv, String uf, java.util.Map<String, String> munMap) throws IOException {
        List<Estabelecimento> batch = new ArrayList<>(2000);
        long total = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(csv.toPath()), StandardCharsets.ISO_8859_1))) {
            String header = br.readLine(); // pula cabeçalho
            String linha;
            while ((linha = br.readLine()) != null) {
                // Layout: CNPJ_BASICO;ORDem;DV;IDENT_MATRIZ_FILIAL;...
                // Endereço aparece a partir do campo TIPO LOGRADOURO.
                // Índices conforme NOVOLAYOUT (separador ';'):
                // 0 cnpj_basico,1 ordem,2 dv,3 matriz_filial,4 nome_fantasia,
                // 5 situacao,6 data_situacao,7 motivo,8 cidade_exterior,9 pais,
                // 10 inicio_ativ,11 cnae_principal,12 cnae_secundaria,
                // 13 tipo_log,14 logradouro,15 numero,16 complemento,
                // 17 bairro,18 cep,19 uf,20 municipio, ...
                String[] f = linha.split(";", -1);
                if (f.length < 21) continue;
                if (!f[19].equalsIgnoreCase(uf)) continue; // segurança

                Estabelecimento e = new Estabelecimento();
                e.setCnpj((f[0] + f[1] + f[2]).trim());
                e.setUf(f[19].trim().toUpperCase());
                e.setMunicipio(f[20].trim());
                e.setNomeMunicipio(munMap.getOrDefault(f[20].trim(), ""));
                e.setTipoLogradouro(f[13].trim());
                e.setLogradouro(f[14].trim());
                e.setNumero(f[15].trim().isEmpty() ? "S/N" : f[15].trim());
                e.setComplemento(f[16].trim());
                e.setBairro(f[17].trim());
                e.setCep(f[18].replaceAll("\\D", "").trim());
                e.setNomeFantasia(f[4].trim());
                e.setSituacaoCadastral(f[5].trim());
                e.setCnaePrincipal(f[11].trim());
                // razaoSocial vem do arquivo EMPRESA; fica nulo aqui (pode ser enriquecido depois)
                batch.add(e);

                if (batch.size() >= 2000) {
                    repository.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                repository.saveAll(batch);
                total += batch.size();
            }
        }
        return total;
    }
}
