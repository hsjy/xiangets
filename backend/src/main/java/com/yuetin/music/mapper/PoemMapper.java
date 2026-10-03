package com.yuetin.music.mapper;

import com.yuetin.music.entity.Poem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PoemMapper {

    /** 诗选：朝代/情绪/关键词（题名、诗人名、诗行内容）可组合筛选 */
    List<Poem> search(@Param("dynasty") String dynasty,
                      @Param("mood") String mood,
                      @Param("keyword") String keyword);

    Poem findById(@Param("id") Long id);

    /** 某诗人的存诗，按 id 升序（近似创作顺序的稳定排列） */
    List<Poem> findByPoet(@Param("poetId") Long poetId);

    /** 今日一诗：按 dayOfYear 确定性轮换 */
    Poem findByOffset(@Param("offset") long offset);

    long countAll();

    /** 语料库分页浏览：tier 为空表示全部层级 */
    List<Poem> pageSearch(@Param("dynasty") String dynasty,
                          @Param("mood") String mood,
                          @Param("keyword") String keyword,
                          @Param("tier") Integer tier,
                          @Param("offset") long offset,
                          @Param("limit") int limit);

    long countSearch(@Param("dynasty") String dynasty,
                     @Param("mood") String mood,
                     @Param("keyword") String keyword,
                     @Param("tier") Integer tier);

    /** 精选层数量（今日/随机一诗只在精选层轮换） */
    long countCurated();

    Poem findByOffsetCurated(@Param("offset") long offset);

    /** 心境筛选项（库端去重） */
    List<String> distinctMoods();
}