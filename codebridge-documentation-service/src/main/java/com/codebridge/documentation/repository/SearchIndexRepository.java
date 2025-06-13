package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.ApiDocumentation;
import com.codebridge.documentation.model.SearchIndex;
import com.codebridge.documentation.model.SearchIndexType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing search index entries.
 */
@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, UUID> {

    /**
     * Find index entries by documentation.
     *
     * @param documentation the API documentation
     * @return the list of search index entries
     */
    List<SearchIndex> findByDocumentation(ApiDocumentation documentation);

    /**
     * Find index entries by type.
     *
     * @param type the index type
     * @return the list of search index entries
     */
    List<SearchIndex> findByType(SearchIndexType type);

    /**
     * Find index entries by documentation and type.
     *
     * @param documentation the API documentation
     * @param type the index type
     * @return the list of search index entries
     */
    List<SearchIndex> findByDocumentationAndType(ApiDocumentation documentation, SearchIndexType type);

    /**
     * Find index entries by content containing the query.
     *
     * @param query the search query
     * @return the list of search index entries
     */
    List<SearchIndex> findByContentContainingIgnoreCase(String query);

    /**
     * Find index entries by title containing the query.
     *
     * @param query the search query
     * @return the list of search index entries
     */
    List<SearchIndex> findByTitleContainingIgnoreCase(String query);

    /**
     * Find index entries by description containing the query.
     *
     * @param query the search query
     * @return the list of search index entries
     */
    List<SearchIndex> findByDescriptionContainingIgnoreCase(String query);
}

