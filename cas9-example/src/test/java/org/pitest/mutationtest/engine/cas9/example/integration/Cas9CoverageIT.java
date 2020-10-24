package org.pitest.mutationtest.engine.cas9.example.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Map;
import javax.xml.bind.JAXBException;
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
        () -> assertEquals(1, coverage.get("AOR").get("KILLED")),
        () -> assertEquals(4, coverage.get("ROR").get("KILLED")),
        () -> assertEquals(3, coverage.get("ROR").get("SURVIVED")),
        () -> assertEquals(3, coverage.get("UOI").get("SURVIVED")));
  }

  @Test
  void shouldGenerateExpectedSbrMutationCoverageReport() {
    assertAll(
        () -> assertEquals(2, coverage.get("SBR").get("KILLED")),
        () -> assertEquals(2, coverage.get("SBR").get("SURVIVED")),
        () -> assertEquals(1, coverage.get("SBR").get("RUN_ERROR")));
  }
}
