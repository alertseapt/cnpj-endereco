package com.cnpjendereco.api.controller;

import com.cnpjendereco.api.service.EtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/etl")
public class EtlController {

    private final EtlService etlService;

    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/{uf}")
    public ResponseEntity<?> importar(@PathVariable String uf) {
        if (uf == null || uf.length() != 2) {
            return ResponseEntity.badRequest().body("UF inválida (use 2 letras, ex.: SP)");
        }
        EtlService.EtlResult r = etlService.importarUf(uf);
        if (r.erro() != null) {
            return ResponseEntity.status(500).body(r);
        }
        return ResponseEntity.ok(r);
    }
}
