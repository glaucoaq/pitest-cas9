package org.pitest.mutationtest.engine.cas9.example.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Cas9CoverageIT {

  private Map<String, Map<String, Long>> coverage;

  @BeforeEach
  void setUp() throws Exception {
    final File file = new File(System.getProperty("pitest.report.file"));
    coverage = Mutations.loadFromXml(file);
  }

  @Test
  void shouldGenerateExpectedCoverageReport() {
    assertAll(
        () -> assertEquals(1, coverage.get("AOR").get("KILLED"), "AOR [KILLED]"),
        () -> assertNull(coverage.get("AOR").get("SURVIVED"), "AOR [SURVIVED]"),
        () -> assertEquals(7, coverage.get("ROR").get("KILLED"), "ROR [KILLED]"),
        () -> assertEquals(2, coverage.get("ROR").get("SURVIVED"), "ROR [SURVIVED]"),
        () -> assertEquals(2, coverage.get("UOI").get("KILLED"), "UOI [KILLED]"),
        () -> assertEquals(1, coverage.get("UOI").get("SURVIVED"), "UOI [SURVIVED]")
    );
  }

  @Test
  void shouldGenerateExpectedSbrMutationCoverageReport() {
    assertAll(
        () -> assertEquals(2, coverage.get("SBR").get("KILLED")),
        () -> assertEquals(2, coverage.get("SBR").get("SURVIVED")),
        () -> assertEquals(1, coverage.get("SBR").get("RUN_ERROR")));
  }
}
