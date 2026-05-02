import { useCallback, useState } from 'react';
import { useDashboardStore } from '../stores/useDashboardStore';
import type { MaterialItem } from '../types/dashboard';

export function useMaterials() {
  const {
    materials,
    selectedMaterialIds,
    materialFilter,
    apiState,
    setMaterialFilter,
    toggleMaterial,
    refreshMaterials,
    uploadMaterial,
  } = useDashboardStore();

  const [loading, setLoading] = useState(false);

  const handleRefresh = useCallback(async () => {
    setLoading(true);
    try {
      await refreshMaterials();
    } finally {
      setLoading(false);
    }
  }, [refreshMaterials]);

  const handleUpload = useCallback(async (file: File) => {
    setLoading(true);
    try {
      await uploadMaterial(file);
    } finally {
      setLoading(false);
    }
  }, [uploadMaterial]);

  const filteredMaterials = materials.filter((material: MaterialItem) => {
    if (materialFilter === 'all') return true;
    if (materialFilter === 'ready') return material.status === 'ready';
    if (materialFilter === 'parsing') return material.status === 'parsing';
    return true;
  });

  const selectedMaterials = materials.filter((material: MaterialItem) =>
    selectedMaterialIds.includes(material.id)
  );

  return {
    materials,
    filteredMaterials,
    selectedMaterials,
    selectedMaterialIds,
    materialFilter,
    apiState,
    loading,
    setMaterialFilter,
    toggleMaterial,
    refreshMaterials: handleRefresh,
    uploadMaterial: handleUpload,
  };
}
