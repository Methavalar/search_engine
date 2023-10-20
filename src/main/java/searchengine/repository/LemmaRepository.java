package searchengine.repository;

import searchengine.model.Lemma;
import searchengine.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findBySiteId(Website website);
    long countBySiteId(Website website);
    @Query(value = "SELECT l.* FROM lemma l where l.lemma IN :lemmas AND l.site_id = :site", nativeQuery = true)
    List<Lemma> findLemmaListBySite(@Param("lemmas") List<String> lemmaList,
                                    @Param("site") Website site);
}
