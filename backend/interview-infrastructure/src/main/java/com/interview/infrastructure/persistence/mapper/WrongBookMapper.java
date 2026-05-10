package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.WrongBook;
import com.interview.infrastructure.persistence.entity.WrongBookPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WrongBookMapper {

    int insert(WrongBookPO wrongBookPO);

    WrongBook selectById(Long id);

    WrongBook selectByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);

    List<WrongBook> selectByUserId(@Param("userId") Long userId);

    List<WrongBook> selectByUserIdAndMasteryStatus(@Param("userId") Long userId, @Param("masteryStatus") String masteryStatus);

    int updateMasteryStatus(@Param("id") Long id, @Param("masteryStatus") String masteryStatus);

    int incrementWrongCount(@Param("id") Long id);

    int updateNotes(@Param("id") Long id, @Param("notes") String notes);

    int deleteById(Long id);
}
