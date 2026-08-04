package com.cnpjendereco.api.service;

import com.cnpjendereco.api.model.Estabelecimento;
import com.cnpjendereco.api.repository.EstabelecimentoRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ETL da Receita Federal (dados abertos, gratuitos).
 * Layout NOVO: os estabelecimentos vêm em 10 arquivos nacionais
 * (Estabelecimentos0.zip .. Estabelecimentos9.zip) + Municipios.zip.
 * Nao ha mais separacao por UF; filtramos pelo campo UF (indice 19).
 * Fonte (espelho com CDN): dados-abertos-rf-cnpj.casadosdados.com.br
 */
@Service
public class EtlService {

    private final EstabelecimentoRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final String dataDir;

    // Espelho Casa dos Dados (mais rápido / estável). Pasta = release mensal.
    // Atualize a data para a release mais recente quando necessário.
    private static final String BASE_URL =
        "https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos/2026-07-12/";
    private static final int NUM_PARTS = 10;

    private static final String INSERT_SQL =
        "INSERT IGNORE INTO estabelecimentos (cnpj, uf, municipio, nome_municipio, bairro, " +
        "tipo_logradouro, logradouro, numero, complemento, cep, nome_fantasia, " +
        "situacao_cadastral, cnae_principal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public EtlService(EstabelecimentoRepository repository, JdbcTemplate jdbcTemplate,
                      @Value("${cnpj.data.dir:/data}") String dataDir) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataDir = dataDir;
    }

    @PostConstruct
    void init() {
        new File(dataDir).mkdirs();
        new File(dataDir, "unzip").mkdirs();
    }

    public record EtlResult(long importados, String arquivo, String erro) {}

    public EtlResult importarUf(String uf) {
        uf = uf.toUpperCase();
        try {
            Map<String, String> munMap = carregarMunicipios();
            repository.deleteByUf(uf);
            long total = 0;
            for (int i = 0; i < NUM_PARTS; i++) {
                String zipName = "Estabelecimentos" + i + ".zip";
                File zipFile = new File(dataDir, zipName);
                downloadIfMissing(zipFile, BASE_URL + zipName);
                if (!zipFile.exists() || zipFile.length() == 0) continue;
                total += processarParte(zipFile, uf, munMap, i);
            }
            return new EtlResult(total, "Estabelecimentos0-9.zip", null);
        } catch (Exception e) {
            return new EtlResult(0, "Estabelecimentos0-9.zip", e.getMessage());
        }
    }

    // Processa a parte direto do zip (sem extrair para disco): lê o entry
    // ESTABELE via stream, filtra pela UF e faz bulk insert em lotes.
    private long processarParte(File zipFile, String uf, Map<String, String> munMap, int parte) throws IOException {
        long total = 0;
        List<Object[]> batch = new ArrayList<>(2000);
        try (ZipFile zf = new ZipFile(zipFile)) {
            var en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName().toUpperCase();
                if (!name.contains("ESTABELE") || entry.isDirectory()) continue;
                long lidas = 0;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(zf.getInputStream(entry), StandardCharsets.ISO_8859_1))) {
                    br.readLine(); // header
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        String[] f = linha.split(";", -1);
                        for (int k = 0; k < f.length; k++) f[k] = f[k].trim().replace("\"", "");
                        if (f.length < 21) continue;
                        if (!f[19].equalsIgnoreCase(uf)) continue;
                        lidas++;
                        batch.add(new Object[]{
                            (f[0] + f[1] + f[2]).trim(),
                            f[19].trim().toUpperCase(),
                            f[20].trim(),
                            munMap.getOrDefault(f[20].trim(), ""),
                            f[17].trim(),
                            f[13].trim(),
                            f[14].trim(),
                            f[15].trim().isEmpty() ? "S/N" : f[15].trim(),
                            f[16].trim(),
                            f[18].replaceAll("\\D", "").trim(),
                            f[4].trim(),
                            f[5].trim(),
                            f[11].trim()
                        });
                        if (batch.size() >= 2000) {
                            jdbcTemplate.batchUpdate(INSERT_SQL, batch);
                            total += batch.size();
                            batch.clear();
                        }
                    }
                }
                System.out.println("[ETL] parte " + parte + " (ES encontrados nesta parte): " + lidas + " | acumulado: " + total);
            }
        }
        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_SQL, batch);
            total += batch.size();
        }
        return total;
    }

    private void downloadIfMissing(File target, String urlStr) throws IOException {
        if (target.exists() && target.length() > 0) return;
        URL url = new URL(urlStr);
        FileUtils.copyURLToFile(url, target, 30000, 900000); // timeout 15min
    }

    private Map<String, String> carregarMunicipios() throws IOException {
        Map<String, String> map = new HashMap<>();
        File zip = new File(dataDir, "Municipios.zip");
        downloadIfMissing(zip, BASE_URL + "Municipios.zip");
        if (!zip.exists() || zip.length() == 0) return map;
        try (ZipFile zf = new ZipFile(zip)) {
            var en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().toUpperCase().contains("MUNICIPIO") || entry.isDirectory()) continue;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(zf.getInputStream(entry), StandardCharsets.ISO_8859_1))) {
                    br.readLine(); // header
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        String[] f = linha.split(";", -1);
                        for (int k = 0; k < f.length; k++) f[k] = f[k].trim().replace("\"", "");
                        if (f.length < 2) continue;
                        map.put(f[0].trim(), f[1].trim());
                    }
                }
                break;
            }
        }
        return map;
    }
}
