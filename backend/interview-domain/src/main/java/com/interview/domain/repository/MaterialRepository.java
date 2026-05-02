package com.interview.domain.repository;

import com.interview.domain.model.Material;

import java.util.List;

public interface MaterialRepository {

    Material save(Long userId, String name, String fileType, String storageUrl);

    List<Material> findByUserIdAndIds(Long userId, List<Long> ids);

    List<Material> findByUserId(Long userId);
}
