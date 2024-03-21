package com.architecture.admin.models.dao.search;

import com.architecture.admin.models.dto.search.SearchLogDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface SearchDao {

    void insertSearchLog(SearchLogDto searchLogDto);
}
