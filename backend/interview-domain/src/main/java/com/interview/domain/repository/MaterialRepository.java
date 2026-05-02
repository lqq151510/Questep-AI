package com.interview.domain.repository;

import com.interview.domain.model.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository {

    Material save(Long userId, String name, String fileType, String storageUrl);

    List<Material> findByUserIdAndIds(Long userId, List<Long> ids);

    List<Material> findByUserId(Long userId);

    Optional<Material> findById(Long id);
}
