package com.interview.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncTaskRecordRepositoryImplTest {

    @Test
    void normalizeStatusShouldMapProcessingToRunning() {
        assertEquals("RUNNING", AsyncTaskRecordRepositoryImpl.normalizeStatus("PROCESSING"));
    }

    @Test
    void normalizeStatusShouldKeepRunningUnchanged() {
        assertEquals("RUNNING", AsyncTaskRecordRepositoryImpl.normalizeStatus("RUNNING"));
    }
}
