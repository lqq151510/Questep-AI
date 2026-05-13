package com.interview.application.service;

import com.interview.application.dto.AddWrongBookCommand;
import com.interview.application.dto.UpdateMasteryCommand;
import com.interview.application.dto.WrongBookItem;
import com.interview.domain.model.Question;
import com.interview.domain.model.WrongBook;
import com.interview.domain.repository.QuestionRepository;
import com.interview.domain.repository.WrongBookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrongBookApplicationServiceTest {

    @Mock
    private WrongBookRepository wrongBookRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("addWrongBook should increment existing entry")
    void addWrongBookShouldIncrementExistingEntry() {
        WrongBookApplicationService service = new WrongBookApplicationService(wrongBookRepository, questionRepository);
        WrongBook existing = wrongBook(1L, 2L, 3);
        WrongBook updated = wrongBook(1L, 2L, 4);
        Question question = question(2L, "什么是线程安全");

        when(wrongBookRepository.findByUserIdAndQuestionId(1L, 2L)).thenReturn(Optional.of(existing));
        when(wrongBookRepository.incrementWrongCount(1L)).thenReturn(updated);
        when(questionRepository.findById(2L)).thenReturn(Optional.of(question));

        WrongBookItem item = service.addWrongBook(1L, new AddWrongBookCommand(2L));

        assertEquals(4, item.wrongCount());
        assertEquals("什么是线程安全", item.question());
        verify(wrongBookRepository).incrementWrongCount(1L);
    }

    @Test
    @DisplayName("listWrongBooks should enrich question fields")
    void listWrongBooksShouldEnrichQuestions() {
        WrongBookApplicationService service = new WrongBookApplicationService(wrongBookRepository, questionRepository);
        WrongBook wb = wrongBook(10L, 30L, 2);
        Question question = question(30L, "解释 volatile");

        when(wrongBookRepository.findByUserId(9L)).thenReturn(List.of(wb));
        when(questionRepository.selectByIds(List.of(30L))).thenReturn(List.of(question));

        List<WrongBookItem> result = service.listWrongBooks(9L);

        assertEquals(1, result.size());
        assertEquals("解释 volatile", result.getFirst().question());
    }

    @Test
    @DisplayName("deleteWrongBook should throw when record does not belong to user")
    void deleteWrongBookShouldThrowWhenMissing() {
        WrongBookApplicationService service = new WrongBookApplicationService(wrongBookRepository, questionRepository);
        when(wrongBookRepository.findByUserId(5L)).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteWrongBook(5L, 99L));
        assertEquals("Wrong book entry not found: 99", ex.getMessage());
    }

    @Test
    @DisplayName("updateMasteryStatus should update existing item")
    void updateMasteryStatusShouldUpdateExistingItem() {
        WrongBookApplicationService service = new WrongBookApplicationService(wrongBookRepository, questionRepository);
        WrongBook original = wrongBook(7L, 11L, 1);
        WrongBook updated = new WrongBook(
                7L,
                1L,
                11L,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now(),
                1,
                "MASTERED",
                LocalDateTime.now(),
                "ok",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now()
        );
        Question question = question(11L, "JMM 是什么");

        when(wrongBookRepository.findByUserId(1L)).thenReturn(List.of(original));
        when(wrongBookRepository.updateMasteryStatus(7L, "MASTERED")).thenReturn(updated);
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));

        WrongBookItem result = service.updateMasteryStatus(1L, 7L, new UpdateMasteryCommand("MASTERED"));

        assertEquals("MASTERED", result.masteryStatus());
        assertEquals("JMM 是什么", result.question());
        verify(wrongBookRepository).updateMasteryStatus(7L, "MASTERED");
    }

    private WrongBook wrongBook(Long id, Long questionId, int wrongCount) {
        return new WrongBook(
                id,
                1L,
                questionId,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                wrongCount,
                "LEARNING",
                null,
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
    }

    private Question question(Long id, String stem) {
        return new Question(
                id,
                100L,
                1L,
                "SHORT_ANSWER",
                stem,
                "answer",
                "analysis",
                3,
                "AI",
                "test-model",
                null,
                null,
                LocalDateTime.now(),
                null,
                LocalDateTime.now().plusDays(1),
                "APPROVED",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
