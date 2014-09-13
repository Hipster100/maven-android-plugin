package com.jayway.maven.plugins.android;

import java.util.Arrays;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenInstallations;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(MavenJUnitTestRunner.class)
@MavenInstallations({"target/maven-installation/apache-maven-3.2.3"})
@MavenVersions({"3.0.5", "3.2.3"})
public abstract class AbstractAndroidMojoIntegrationTest {

  @Rule
  public final TestResources resources = null;
  
  public final MavenRuntime verifier;

  public AbstractAndroidMojoIntegrationTest(MavenRuntimeBuilder builder) throws Exception {
    this.verifier = builder.withCliOptions("-X").build();
  }
}
