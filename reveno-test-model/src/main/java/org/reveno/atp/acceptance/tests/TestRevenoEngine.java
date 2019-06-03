package org.reveno.atp.acceptance.tests;

import org.reveno.atp.core.Engine;
import org.reveno.atp.core.api.storage.JournalsStorage;

import java.io.File;

public class TestRevenoEngine extends Engine {

    public TestRevenoEngine(File baseDir) {
        super(baseDir);
    }

    public synchronized void roll(Runnable r) {
        journalsManager.roll(0, r);
    }

    public void syncAll() {
        workflowEngine.getPipe().sync();
        eventPublisher.getPipe().sync();
    }

    public JournalsStorage getJournalsStorage() {
        return journalsStorage;
    }

}
