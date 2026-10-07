package com.yuetin.music.mapper;

import com.yuetin.music.entity.Poem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PoemMapper {

    /** 诗选：朝代/情绪/关键词（题名、诗人名、诗行内容）可组合筛选 */
    List<Poem> search(@Param("dynasty") String dynasty,
                      @Param("mood") String mood,
                      @Param("keyword") String keyword,
                      @Param("kwShort") boolean kwShort);

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
                          @Param("kwShort") boolean kwShort,
                          @Param("offset") long offset,
                          @Param("limit") int limit);

    long countSearch(@Param("dynasty") String dynasty,
                     @Param("mood") String mood,
                     @Param("keyword") String keyword,
                     @Param("tier") Integer tier,
                     @Param("kwShort") boolean kwShort);

    /** 精选层数量（今日/随机一诗只在精选层轮换） */
    long countCurated();

    Poem findByOffsetCurated(@Param("offset") long offset);

    /** 心境筛选项（库端去重） */
    List<String> distinctMoods();

    /** 飞花令：含令字诗行总数 */
    long countLinesWithChar(@Param("ch") String ch);

    /** 飞花令：随机偏移取一条含令字诗 */
    Map<String, Object> findLineWithChar(@Param("ch") String ch, @Param("offset") long offset);

    /** 飞花令：校验对句是否存在（去标点包含匹配） */
    long countLineExists(@Param("line") String line);

    /** 语料层按需注译赏：写入三字段并升级为精选层 */
    int updateNotes(@Param("id") Long id, @Param("annotation") String annotation,
                    @Param("translation") String translation, @Param("appreciation") String appreciation);
}