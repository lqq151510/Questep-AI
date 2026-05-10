package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.dto.AddWrongBookCommand;
import com.interview.application.dto.UpdateMasteryCommand;
import com.interview.application.dto.WrongBookItem;
import com.interview.application.service.WrongBookApplicationService;
import com.interview.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wrong-books")
public class WrongBookController {

    private final WrongBookApplicationService wrongBookApplicationService;

    public WrongBookController(WrongBookApplicationService wrongBookApplicationService) {
        this.wrongBookApplicationService = wrongBookApplicationService;
    }

    @PostMapping
    public ApiResponse<WrongBookItem> addWrongBook(@Valid @RequestBody AddWrongBookCommand command) {
        return ApiResponse.ok(wrongBookApplicationService.addWrongBook(CurrentUser.id(), command));
    }

    @GetMapping
    public ApiResponse<List<WrongBookItem>> listWrongBooks(
            @RequestParam(required = false) String masteryStatus
    ) {
        Long userId = CurrentUser.id();
        if (masteryStatus != null && !masteryStatus.isBlank()) {
            return ApiResponse.ok(wrongBookApplicationService.listByMasteryStatus(userId, masteryStatus));
        }
        return ApiResponse.ok(wrongBookApplicationService.listWrongBooks(userId));
    }

    @PutMapping("/{id}/mastery")
    public ApiResponse<WrongBookItem> updateMasteryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMasteryCommand command
    ) {
        return ApiResponse.ok(wrongBookApplicationService.updateMasteryStatus(CurrentUser.id(), id, command));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWrongBook(@PathVariable Long id) {
        wrongBookApplicationService.deleteWrongBook(CurrentUser.id(), id);
        return ApiResponse.ok(null);
    }
}
