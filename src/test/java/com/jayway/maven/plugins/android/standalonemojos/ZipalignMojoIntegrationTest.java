package com.jayway.maven.plugins.android.standalonemojos;


import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenInstallations;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jayway.maven.plugins.android.AbstractAndroidMojoIntegrationTest;

@RunWith(MavenJUnitTestRunner.class)
@MavenInstallations({"target/maven-installation/apache-maven-3.2.3"})
@MavenVersions({"3.0.5", "3.2.3"})
public class ZipalignMojoIntegrationTest {
  
  @Rule
  public final TestResources resources = new TestResources();
  
  public final MavenRuntime verifier;
  
  public ZipalignMojoIntegrationTest(MavenRuntimeBuilder builder) throws Exception {
    this.verifier = builder.withCliOptions("-X").build();
  }
  
  
  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("non-android-project");

//    File basedir = new File("target/verifier/", getClass().getSimpleName() + "/" + "testVerifier");
//    basedir.mkdirs();
    
    
    MavenExecutionResult result = verifier
          .forProject(basedir)
          // switch on debug logging
          // .withCliOptions("-X") // somehow this is not on the classpath, must have old version or so
          //.execute("compile");
    .execute("com.jayway.maven.plugins.android.generation2:android-maven-plugin:zipalign");

    result.assertErrorFreeLog();

    result.assertLogText("Skipping zipalign on jar"); 

    //TestResources.assertFilesPresent(basedir, "target/sample/sample.txt");
  }

}
