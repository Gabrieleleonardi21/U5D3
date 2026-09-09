package it.epicode.u5d3.repository;

import it.epicode.u5d3.model.Documento;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Interfaccia vuota per scelta: JpaRepository porta gia' findAll, findById,
 * save, deleteById e la paginazione. Spring ne crea l'implementazione all'avvio,
 * non va scritta.
 *
 * I due tipi fra parentesi sono l'entita' gestita e il tipo della sua chiave
 * primaria: UUID e non Long, altrimenti findById(...) non compila.
 */
public interface DocumentoRepository extends JpaRepository<Documento, UUID> {
}
