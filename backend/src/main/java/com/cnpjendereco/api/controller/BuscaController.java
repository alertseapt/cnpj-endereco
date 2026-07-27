package com.cnpjendereco.api.controller;

import com.cnpjendereco.api.dto.BuscaRequest;
import com.cnpjendereco.api.dto.EstabelecimentoResponse;
import com.cnpjendereco.api.service.BuscaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/busca")
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @PostMapping
    public ResponseEntity<?> buscar(@Valid @RequestBody BuscaRequest req,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        // UF e Município obrigatórios (validação explícita, além do @NotNull no DTO se houver)
        if (req.getUf() == null || req.getUf().isBlank()) {
            return ResponseEntity.badRequest().body("UF é obrigatório");
        }
        if (req.getMunicipio() == null || req.getMunicipio().isBlank()) {
            return ResponseEntity.badRequest().body("Município é obrigatório");
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 100));
        Page<EstabelecimentoResponse> result = buscaService.buscar(req, pageable);
        return ResponseEntity.ok(result);
    }
}
