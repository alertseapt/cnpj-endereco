export async function buscarCnpjs(payload, page = 0, size = 20) {
  const resp = await fetch(`/api/busca?page=${page}&size=${size}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  if (!resp.ok) {
    const msg = await resp.text();
    throw new Error(msg || ('Erro ' + resp.status));
  }
  return resp.json(); // {content: [...], totalElements, totalPages, number}
}

export async function importarUf(uf) {
  const resp = await fetch(`/api/etl/${uf}`, { method: 'POST' });
  return resp.json();
}
