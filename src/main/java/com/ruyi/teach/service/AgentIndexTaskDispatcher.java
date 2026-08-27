package com.ruyi.teach.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class AgentIndexTaskDispatcher {

    private final Executor executor;

    public AgentIndexTaskDispatcher(@Qualifier("agentIndexExecutor") Executor executor) {
        this.executor = executor;
    }

    public void dispatchAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(task);
                }
            });
            return;
        }
        dispatch(task);
    }

    private void dispatch(Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.warn("Agent index task failed asynchronously, cause={}",
                            e.getClass().getSimpleName());
                    log.debug("Agent index task failure details", e);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            log.warn("Agent index task rejected because the bounded queue is full");
        }
    }
}
