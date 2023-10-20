package searchengine.repository;

import searchengine.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteRepository extends JpaRepository<Website, Integer> {
    Website findByUrl(String url);
}
