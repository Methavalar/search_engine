package searchengine.repository;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(value = "SELECT i.* FROM indexs i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<Index> findByPagesAndLemmas(@Param("lemmas")List<Lemma> lemmaList,
                                     @Param("pages")List<Page> pageList);
}
