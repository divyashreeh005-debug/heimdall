package com.heimdall;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build if a module reaches into another module's internals
 * instead of going through its public API - this is what actually
 * enforces the boundaries drawn in docs/01-c4-architecture.md Level 3.
 */
class ModularityTest {

    @Test
    void verifiesModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(HeimdallApplication.class);
        modules.verify();
    }
}
