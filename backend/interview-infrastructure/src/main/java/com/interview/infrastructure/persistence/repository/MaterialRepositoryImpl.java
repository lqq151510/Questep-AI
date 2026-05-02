package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.Material;
import com.interview.domain.repository.MaterialRepository;
import com.interview.infrastructure.persistence.entity.MaterialPO;
import com.interview.infrastructure.persistence.mapper.MaterialMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MaterialRepositoryImpl implements MaterialRepository {

    private final MaterialMapper materialMapper;

    public MaterialRepositoryImpl(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    @Override
    public Material save(Long userId, String name, String fileType, String storageUrl) {
        MaterialPO po = new MaterialPO();
        po.setUserId(userId);
        po.setMaterialName(name);
        po.setMaterialType(fileType);
        po.setStorageUrl(storageUrl);
        po.setParseStatus("PENDING");
        materialMapper.insert(po);
        return materialMapper.selectById(po.getId());
    }

    @Override
    public List<Material> findByUserIdAndIds(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return materialMapper.selectByUserIdAndIds(userId, ids);
    }

    @Override
    public List<Material> findByUserId(Long userId) {
        return materialMapper.selectByUserId(userId);
    }

    @Override
    public Optional<Material> findById(Long id) {
        return Optional.ofNullable(materialMapper.selectById(id));
    }
}
