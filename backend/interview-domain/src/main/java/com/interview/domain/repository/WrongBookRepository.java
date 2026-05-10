package com.interview.domain.repository;

import com.interview.domain.model.WrongBook;

import java.util.List;
import java.util.Optional;

public interface WrongBookRepository {

    WrongBook save(Long userId, Long questionId);

    Optional<WrongBook> findByUserIdAndQuestionId(Long userId, Long questionId);

    List<WrongBook> findByUserId(Long userId);

    List<WrongBook> findByUserIdAndMasteryStatus(Long userId, String masteryStatus);

    WrongBook updateMasteryStatus(Long id, String masteryStatus);

    WrongBook incrementWrongCount(Long id);

    WrongBook updateNotes(Long id, String notes);

    int deleteById(Long id);
}
