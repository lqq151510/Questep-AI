package com.interview.application.service;

import com.interview.application.dto.AddWrongBookCommand;
import com.interview.application.dto.UpdateMasteryCommand;
import com.interview.application.dto.WrongBookItem;
import com.interview.domain.model.Question;
import com.interview.domain.model.WrongBook;
import com.interview.domain.repository.QuestionRepository;
import com.interview.domain.repository.WrongBookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WrongBookApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(WrongBookApplicationService.class);

    private final WrongBookRepository wrongBookRepository;
    private final QuestionRepository questionRepository;

    public WrongBookApplicationService(
            WrongBookRepository wrongBookRepository,
            QuestionRepository questionRepository
    ) {
        this.wrongBookRepository = wrongBookRepository;
        this.questionRepository = questionRepository;
    }

    public WrongBookItem addWrongBook(Long userId, AddWrongBookCommand command) {
        Optional<WrongBook> existing = wrongBookRepository.findByUserIdAndQuestionId(userId, command.questionId());
        if (existing.isPresent()) {
            WrongBook updated = wrongBookRepository.incrementWrongCount(existing.get().id());
            logger.info("Incremented wrong count for existing wrong book entry: userId={}, questionId={}", userId, command.questionId());
            Question question = questionRepository.findById(command.questionId()).orElse(null);
            return WrongBookItem.from(updated, question);
        }

        WrongBook saved = wrongBookRepository.save(userId, command.questionId());
        logger.info("Added new wrong book entry: userId={}, questionId={}", userId, command.questionId());
        Question question = questionRepository.findById(command.questionId()).orElse(null);
        return WrongBookItem.from(saved, question);
    }

    public List<WrongBookItem> listWrongBooks(Long userId) {
        List<WrongBook> wrongBooks = wrongBookRepository.findByUserId(userId);
        return enrichWithQuestions(wrongBooks);
    }

    public List<WrongBookItem> listByMasteryStatus(Long userId, String masteryStatus) {
        List<WrongBook> wrongBooks = wrongBookRepository.findByUserIdAndMasteryStatus(userId, masteryStatus);
        return enrichWithQuestions(wrongBooks);
    }

    public WrongBookItem updateMasteryStatus(Long userId, Long wrongBookId, UpdateMasteryCommand command) {
        WrongBook wrongBook = wrongBookRepository.findByUserId(userId).stream()
                .filter(wb -> wb.id().equals(wrongBookId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wrong book entry not found: " + wrongBookId));

        WrongBook updated = wrongBookRepository.updateMasteryStatus(wrongBookId, command.masteryStatus());
        Question question = questionRepository.findById(wrongBook.questionId()).orElse(null);
        return WrongBookItem.from(updated, question);
    }

    public void deleteWrongBook(Long userId, Long wrongBookId) {
        boolean exists = wrongBookRepository.findByUserId(userId).stream()
                .anyMatch(wb -> wb.id().equals(wrongBookId));
        if (!exists) {
            throw new IllegalArgumentException("Wrong book entry not found: " + wrongBookId);
        }
        wrongBookRepository.deleteById(wrongBookId);
    }

    private List<WrongBookItem> enrichWithQuestions(List<WrongBook> wrongBooks) {
        if (wrongBooks.isEmpty()) {
            return List.of();
        }

        Set<Long> questionIds = wrongBooks.stream()
                .map(WrongBook::questionId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = questionRepository.selectByIds(List.copyOf(questionIds)).stream()
                .collect(Collectors.toMap(Question::id, q -> q));

        return wrongBooks.stream()
                .map(wb -> WrongBookItem.from(wb, questionMap.get(wb.questionId())))
                .toList();
    }
}
