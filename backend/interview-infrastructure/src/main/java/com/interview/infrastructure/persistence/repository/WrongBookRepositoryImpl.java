package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.WrongBook;
import com.interview.domain.repository.WrongBookRepository;
import com.interview.infrastructure.persistence.entity.WrongBookPO;
import com.interview.infrastructure.persistence.mapper.WrongBookMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class WrongBookRepositoryImpl implements WrongBookRepository {

    private final WrongBookMapper wrongBookMapper;

    public WrongBookRepositoryImpl(WrongBookMapper wrongBookMapper) {
        this.wrongBookMapper = wrongBookMapper;
    }

    @Override
    public WrongBook save(Long userId, Long questionId) {
        WrongBookPO po = new WrongBookPO();
        po.setUserId(userId);
        po.setQuestionId(questionId);
        wrongBookMapper.insert(po);
        return wrongBookMapper.selectById(po.getId());
    }

    @Override
    public Optional<WrongBook> findByUserIdAndQuestionId(Long userId, Long questionId) {
        return Optional.ofNullable(wrongBookMapper.selectByUserIdAndQuestionId(userId, questionId));
    }

    @Override
    public List<WrongBook> findByUserId(Long userId) {
        return wrongBookMapper.selectByUserId(userId);
    }

    @Override
    public List<WrongBook> findByUserIdAndMasteryStatus(Long userId, String masteryStatus) {
        return wrongBookMapper.selectByUserIdAndMasteryStatus(userId, masteryStatus);
    }

    @Override
    public WrongBook updateMasteryStatus(Long id, String masteryStatus) {
        wrongBookMapper.updateMasteryStatus(id, masteryStatus);
        return wrongBookMapper.selectById(id);
    }

    @Override
    public WrongBook incrementWrongCount(Long id) {
        wrongBookMapper.incrementWrongCount(id);
        return wrongBookMapper.selectById(id);
    }

    @Override
    public WrongBook updateNotes(Long id, String notes) {
        wrongBookMapper.updateNotes(id, notes);
        return wrongBookMapper.selectById(id);
    }

    @Override
    public int deleteById(Long id) {
        return wrongBookMapper.deleteById(id);
    }
}
