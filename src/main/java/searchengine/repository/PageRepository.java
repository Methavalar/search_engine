package searchengine.repository;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Iterable<Page> findBySiteId(Website website);
    long countBySiteId(Website website);
    Page findByPath(String path);

    @Query(value = "SELECT p.* FROM page p JOIN indexs i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas",
            nativeQuery = true)
    List<Page> findByLemmas(@Param("lemmas")List<Lemma> lemmaList);
}
