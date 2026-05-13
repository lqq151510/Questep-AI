package com.interview.application.service;

import com.interview.application.port.VectorStoreGateway;
import com.interview.domain.model.MaterialChunk;
import com.interview.domain.repository.MaterialChunkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialRagApplicationServiceTest {

    @Mock
    private MaterialChunkRepository materialChunkRepository;

    @Mock
    private VectorStoreGateway vectorStoreGateway;

    @Test
    @DisplayName("retrieveContext should return vector hit texts when available")
    void retrieveContextShouldReturnVectorHitsWhenAvailable() {
        MaterialRagApplicationService service = new MaterialRagApplicationService(
                materialChunkRepository,
                vectorStoreGateway,
                true,
                128,
                500,
                80
        );
        when(vectorStoreGateway.search(any(), anyInt(), anyLong())).thenReturn(List.of(
                new VectorStoreGateway.VectorSearchHit("1-0", 0.9, "JVM 调优要点", 1L, 0),
                new VectorStoreGateway.VectorSearchHit("1-1", 0.8, "并发容器对比", 1L, 1)
        ));

        List<String> result = service.retrieveContext(1L, "JVM", 2);

        assertEquals(List.of("JVM 调优要点", "并发容器对比"), result);
    }

    @Test
    @DisplayName("retrieveContext should fallback to keyword search when milvus fails")
    void retrieveContextShouldFallbackWhenMilvusFails() {
        MaterialRagApplicationService service = new MaterialRagApplicationService(
                materialChunkRepository,
                vectorStoreGateway,
                true,
                128,
                500,
                80
        );
        when(vectorStoreGateway.search(any(), anyInt(), anyLong()))
                .thenThrow(new IllegalStateException("milvus unavailable"));
        when(materialChunkRepository.findRecentByUserId(2L, 80)).thenReturn(List.of(
                new MaterialChunk(1L, 10L, 0, "java thread state lifecycle", 10, "hashing-v1", "10-0", null, LocalDateTime.now(), LocalDateTime.now()),
                new MaterialChunk(2L, 10L, 1, "redis cache eviction policy", 10, "hashing-v1", "10-1", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        List<String> result = service.retrieveContext(2L, "java thread", 1);

        assertEquals(1, result.size());
        assertEquals("java thread state lifecycle", result.getFirst());
    }
}
