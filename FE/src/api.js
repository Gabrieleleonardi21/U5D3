// Punto unico da cui passano tutte le chiamate al back-end:
// se cambia l'indirizzo o serve aggiungere un header (es. Authorization),
// si modifica solo qui invece che in ogni componente.
const API_URL = import.meta.env.VITE_API_URL

export async function api(percorso, opzioni = {}) {
  const risposta = await fetch(`${API_URL}${percorso}`, {
    ...opzioni,
    headers: { 'Content-Type': 'application/json', ...opzioni.headers },
  })

  // fetch NON lancia errore sugli status 4xx/5xx: senza questo controllo
  // un 500 arriverebbe al componente come se fosse una risposta valida.
  if (!risposta.ok) {
    throw new Error(`Errore ${risposta.status} su ${percorso}`)
  }

  return risposta.json()
}
