package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page", indexes = {@javax.persistence.Index(name = "pathes", columnList = "path")})
@Setter
@Getter
@NoArgsConstructor
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, referencedColumnName = "id")
    private Website siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL)
    private List<Index> indexList = new ArrayList<>();


    public Page(Website siteId, String path, int code, String content){
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
