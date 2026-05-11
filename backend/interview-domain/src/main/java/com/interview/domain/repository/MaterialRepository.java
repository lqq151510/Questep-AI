package com.interview.domain.repository;

import com.interview.domain.model.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository {

    Material save(Long userId, String name, String fileType, String storageUrl);

    List<Material> findByUserIdAndIds(Long userId, List<Long> ids);

    List<Material> findByUserId(Long userId);

    List<Long> findUsersWithParsedMaterials(int limit);

    List<Material> findParsedMaterialsByUser(Long userId, int limit);

    Optional<Material> findById(Long id);

    Optional<Material> findByIdAndUserId(Long id, Long userId);

    void deleteById(Long id);

    void markParseSuccess(Long id, String contentHash, String analysisText);

    void markParseFailure(Long id, String errorMsg);

    void markParsePending(Long id);
}
