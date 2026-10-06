package com.yuetin.music.mapper;

import com.yuetin.music.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    List<User> search(@Param("keyword") String keyword);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") Long id);

    long countAll();

    /** 积分变动：delta 可正可负，结果不低于 0 */
    int addPoints(@Param("id") Long id, @Param("delta") int delta);
}