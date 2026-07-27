<template>
  <div class="container">
    <h1>CNPJ por Endereço</h1>
    <p class="sub">Busque estabelecimentos da Receita Federal por endereço. UF e Município são obrigatórios.</p>

    <form @submit.prevent="onBuscar" class="form">
      <div class="row">
        <label>UF *</label>
        <select v-model="form.uf" required>
          <option value="">Selecione a UF</option>
          <option v-for="u in ufs" :key="u" :value="u">{{ u }}</option>
        </select>
      </div>

      <div class="row">
        <label>Município (código IBGE ou nome) *</label>
        <input v-model="form.municipio" placeholder="ex.: 3550308 ou São Paulo" required />
      </div>

      <details class="opcionais">
        <summary>Filtros opcionais (deixe em branco para não filtrar)</summary>
        <div class="row">
          <label>Logradouro</label>
          <input v-model="form.logradouro" placeholder="ex.: RUA DAS FLORES" />
        </div>
        <div class="row">
          <label>Número</label>
          <input v-model="form.numero" placeholder="ex.: 100" />
        </div>
        <div class="row">
          <label>Bairro</label>
          <input v-model="form.bairro" placeholder="ex.: CENTRO" />
        </div>
        <div class="row">
          <label>CEP</label>
          <input v-model="form.cep" placeholder="ex.: 01001000" />
        </div>
      </details>

      <button type="submit" :disabled="loading">{{ loading ? 'Buscando...' : 'Buscar CNPJs' }}</button>
      <p v-if="erro" class="erro">{{ erro }}</p>
    </form>

    <section v-if="resultado" class="resultado">
      <h2>{{ resultado.totalElements }} estabelecimento(s) encontrado(s)</h2>
      <table>
        <thead>
          <tr>
            <th>CNPJ</th><th>Razão Social</th><th>Endereço</th><th>Situação</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in resultado.content" :key="e.cnpj">
            <td>{{ e.cnpj }}</td>
            <td>{{ e.razaoSocial || e.nomeFantasia || '—' }}</td>
            <td>
              {{ e.tipoLogradouro }} {{ e.logradouro }}, {{ e.numero }}
              <span v-if="e.bairro"> — {{ e.bairro }}</span>
              <span v-if="e.nomeMunicipio"> — {{ e.nomeMunicipio }}/{{ e.uf }}</span>
              <span v-else> — {{ e.uf }}</span>
              <span v-if="e.cep"> — CEP {{ e.cep }}</span>
            </td>
            <td>{{ situacao(e.situacaoCadastral) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="paginacao" v-if="resultado.totalPages > 1">
        <button :disabled="pagina === 0" @click="irPagina(pagina - 1)">Anterior</button>
        <span>Página {{ pagina + 1 }} / {{ resultado.totalPages }}</span>
        <button :disabled="pagina + 1 >= resultado.totalPages" @click="irPagina(pagina + 1)">Próxima</button>
      </div>
    </section>

    <hr />
    <section class="admin">
      <h3>Importar dados de uma UF (ETL Receita Federal)</h3>
      <p class="sub">Baixa e importa os estabelecimentos da UF selecionada. Pode demorar conforme o tamanho.</p>
      <button @click="onImportar" :disabled="importando">
        {{ importando ? 'Importando...' : 'Importar UF ' + (form.uf || '?') }}
      </button>
      <p v-if="importMsg" class="erro">{{ importMsg }}</p>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { buscarCnpjs, importarUf } from '../services/api.js'

const ufs = ['AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
             'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO']

const form = reactive({ uf: '', municipio: '', logradouro: '', numero: '', bairro: '', cep: '' })
const loading = ref(false)
const erro = ref('')
const resultado = ref(null)
const pagina = ref(0)
const importando = ref(false)
const importMsg = ref('')

function situacao(c) {
  return { '01': 'NULA', '2': 'ATIVA', '3': 'SUSPENSA', '4': 'INAPTA', '08': 'BAIXADA' }[c] || c || '—'
}

async function onBuscar() {
  erro.value = ''
  loading.value = true
  pagina.value = 0
  try {
    resultado.value = await buscarCnpjs({ ...form }, 0, 20)
  } catch (e) {
    erro.value = e.message
    resultado.value = null
  } finally {
    loading.value = false
  }
}

async function irPagina(p) {
  pagina.value = p
  loading.value = true
  try {
    resultado.value = await buscarCnpjs({ ...form }, p, 20)
  } catch (e) {
    erro.value = e.message
  } finally {
    loading.value = false
  }
}

async function onImportar() {
  if (!form.uf) { importMsg.value = 'Selecione a UF primeiro.'; return }
  importando.value = true
  importMsg.value = ''
  try {
    const r = await importarUf(form.uf)
    importMsg.value = r.erro ? ('Falha: ' + r.erro) : (`Importado(s): ${r.importados} registro(s) do arquivo ${r.arquivo}`)
  } catch (e) {
    importMsg.value = 'Erro: ' + e.message
  } finally {
    importando.value = false
  }
}
</script>

<style>
.container { max-width: 900px; margin: 0 auto; padding: 24px; font-family: system-ui, sans-serif; }
h1 { color: #1a3c5e; }
.sub { color: #555; margin-bottom: 20px; }
.form { background: #f6f8fa; padding: 18px; border-radius: 8px; }
.row { display: flex; flex-direction: column; margin-bottom: 12px; }
.row label { font-weight: 600; margin-bottom: 4px; }
input, select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
button { background: #1a3c5e; color: #fff; border: 0; padding: 10px 16px; border-radius: 4px; cursor: pointer; }
button:disabled { opacity: 0.6; }
.erro { color: #b00; margin-top: 8px; }
.opcionais { margin: 12px 0; }
.opcionais summary { cursor: pointer; font-weight: 600; }
.resultado { margin-top: 24px; }
table { width: 100%; border-collapse: collapse; margin-top: 12px; }
th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; font-size: 13px; }
.paginacao { display: flex; gap: 12px; align-items: center; margin-top: 12px; }
.admin { margin-top: 32px; }
</style>
