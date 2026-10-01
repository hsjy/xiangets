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
}
