package com.ruyi.teach.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AgentIndexTaskDispatcherTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void waitsForCommitBeforeDispatchingIndexTask() {
        AgentIndexTaskDispatcher dispatcher = new AgentIndexTaskDispatcher(Runnable::run);
        AtomicBoolean executed = new AtomicBoolean();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatchAfterCommit(() -> executed.set(true));

        assertThat(executed).isFalse();
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertThat(executed).isTrue();
    }

    @Test
    void dispatchesImmediatelyWithoutTransaction() {
        AgentIndexTaskDispatcher dispatcher = new AgentIndexTaskDispatcher(Runnable::run);
        AtomicBoolean executed = new AtomicBoolean();

        dispatcher.dispatchAfterCommit(() -> executed.set(true));

        assertThat(executed).isTrue();
    }
}
