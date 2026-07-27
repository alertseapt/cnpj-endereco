# CNPJ por Endereço

Busca estabelecimentos da Receita Federal (dados abertos, gratuitos) por endereço.
O usuário informa UF + Município (obrigatórios) e, opcionalmente, logradouro,
número, bairro e CEP para refinar. Lista os CNPJs naquele endereço.

## Stack
- Backend: Java 21 + Spring Boot 3.5.3 + Gradle
- Frontend: Vue 3 + Vite, servido por Nginx
- Banco: MySQL 8.0
- Deploy: Docker Compose + Dokploy (rede dokploy-network, Traefik injetado)

## Estrutura
```
cnpj-endereco/
├── backend/      # Spring Boot API
├── frontend/     # Vue 3 SPA
└── docker-compose.prod.yml
```

## Fonte dos dados
Receita Federal — Dados Abertos de CNPJ:
https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9
O ETL baixa `ESTABELECIMENTO_<UF>.zip` por UF sob demanda (endpoint POST /api/etl/{uf}).

## Busca (regra de negócio)
- UF e Município são OBRIGATÓRIOS. Município aceita código IBGE (7 dígitos)
  OU nome da cidade (LIKE em nomeMunicipio) — ex.: "3550308" ou "São Paulo".
- Logradouro, número, bairro, CEP são OPCIONAIS (filtro progressivo mais fino).
- Endpoints:
  - POST /api/busca  (body: uf, municipio, [logradouro, numero, bairro, cep])
  - POST /api/etl/{uf}  (importa a UF; baixa ESTABELECIMENTO_<UF>.zip E MUNICIPIOS.zip)
  - GET  /health

## Deploy no Dokploy
1. Subir repo no GitHub.
2. No Dokploy: projeto "CnpjEndereco", app Compose, source GitHub,
   composePath ./docker-compose.prod.yml, composeType docker-compose.
3. Variável de ambiente: DB_ROOT_PASSWORD.
4. Domínios: frontend (ex.: cnpjendereco.com) e api (api.cnpjendereco.com),
   certificado letsencrypt.
5. O Dokploy injeta a rede dokploy-network e labels Traefik automaticamente.

## Build local
docker compose -f docker-compose.prod.yml up --build
