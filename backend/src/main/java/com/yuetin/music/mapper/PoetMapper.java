package com.yuetin.music.mapper;

import com.yuetin.music.entity.Poet;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PoetMapper {

    List<Poet> list(@Param("dynasty") String dynasty);

    Poet findById(@Param("id") Long id);

    /** 诗人名 + 朝代定位（种子幂等用） */
    Poet findByNameAndDynasty(@Param("name") String name, @Param("dynasty") String dynasty);

    int insert(Poet poet);


    /** 诗人分页浏览（语料库全量用） */
    List<Poet> pageList(@Param("dynasty") String dynasty,
                        @Param("keyword") String keyword,
                        @Param("offset") long offset,
                        @Param("limit") int limit);

    long countList(@Param("dynasty") String dynasty,
                   @Param("keyword") String keyword);

    /** 朝代筛选项（库端去重） */
    List<String> distinctDynasties();
}