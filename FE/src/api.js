// Punto unico da cui passano tutte le chiamate al back-end:
// se cambia l'indirizzo o serve aggiungere un header (es. Authorization),
// si modifica solo qui invece che in ogni componente.
const API_URL = import.meta.env.VITE_API_URL

export async function api(percorso, opzioni = {}) {
  const headers = { ...opzioni.headers }

  // Con FormData il Content-Type NON va impostato a mano: il browser deve
  // scriverlo lui perche' ci aggiunge il "boundary" che separa i campi.
  // Forzando application/json, l'upload del file fallirebbe con 415.
  if (!(opzioni.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  const risposta = await fetch(`${API_URL}${percorso}`, { ...opzioni, headers })

  // fetch NON lancia errore sugli status 4xx/5xx: senza questo controllo
  // un 500 arriverebbe al componente come se fosse una risposta valida.
  if (!risposta.ok) {
    throw new Error(await leggiMessaggioErrore(risposta))
  }

  return risposta.json()
}

// Il back-end risponde sempre con { status, messaggio, timestamp } grazie
// all'ExceptionsHandler. Se pero' l'errore arriva da altro (proxy, server
// spento), il corpo non e' JSON: in quel caso ripieghiamo sullo status.
async function leggiMessaggioErrore(risposta) {
  try {
    const corpo = await risposta.json()
    if (corpo.messaggio) {
      return corpo.messaggio
    }
  } catch {
    // corpo non leggibile come JSON: si usa il messaggio generico sotto
  }
  return `Errore ${risposta.status}`
}
