package com.jayway.maven.plugins.android.phase05compile;

import com.jayway.maven.plugins.android.common.AetherHelper;
import com.jayway.maven.plugins.android.common.JarHelper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Various helper methods for dealing with Android Native makefiles.
 *
 * @author Johan Lindquist
 */
public class MakefileHelper
{

    public static final String MAKEFILE_CAPTURE_FILE = "ANDROID_MAVEN_PLUGIN_LOCAL_C_INCLUDES_FILE";

    /**
     * Holder for the result of creating a makefile.  This in particular keep tracks of all directories created
     * for extracted header files.
     */
    public static class MakefileHolder
    {
        String makeFile;
        List<File> includeDirectories;

        public MakefileHolder( List<File> includeDirectories, String makeFile )
        {
            this.includeDirectories = includeDirectories;
            this.makeFile = makeFile;
        }

        public List<File> getIncludeDirectories()
        {
            return includeDirectories;
        }

        public String getMakeFile()
        {
            return makeFile;
        }
    }

    /**
     * Cleans up all include directories created in the temp directory during the build.
     *
     * @param makefileHolder The holder produced by the
     * {@link MakefileHelper#createMakefileFromArtifacts(java.io.File, java.util.Set,
     * boolean, org.sonatype.aether.RepositorySystemSession, java.util.List, org.sonatype.aether.RepositorySystem)}
     */
    public static void cleanupAfterBuild( MakefileHolder makefileHolder )
    {

        if ( makefileHolder.getIncludeDirectories() != null )
        {
            for ( File file : makefileHolder.getIncludeDirectories() )
            {
                try
                {
                    FileUtils.deleteDirectory( file );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Creates an Android Makefile based on the specified set of static library dependency artifacts.
     *
     * @param outputDir         Directory to resolve artifact locations relative to.  Makefiles contain relative paths
     * @param artifacts         The list of (static library) dependency artifacts to create the Makefile from
     * @param useHeaderArchives If true, the Makefile should include a LOCAL_EXPORT_C_INCLUDES statement, pointing to
     *                          the location where the header archive was expanded
     * @param repoSession
     * @param projectRepos
     * @param repoSystem
     * @return The created Makefile
     */
    public static MakefileHolder createMakefileFromArtifacts( File outputDir, Set<Artifact> artifacts,
                                                              boolean useHeaderArchives,
                                                              RepositorySystemSession repoSession,
                                                              List<RemoteRepository> projectRepos,
                                                              RepositorySystem repoSystem )
            throws MojoExecutionException
    {

        final StringBuilder makeFile = new StringBuilder( "# Generated by Android Maven Plugin\n" );
        final List<File> includeDirectories = new ArrayList<File>();

        // Add now output - allows us to somewhat intelligently determine the include paths to use for the header
        // archive
        makeFile.append( "$(shell echo \"LOCAL_C_INCLUDES=$(LOCAL_C_INCLUDES)\" > $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_PATH=$(LOCAL_PATH)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_MODULE_FILENAME=$(LOCAL_MODULE_FILENAME)\" >> $("
                + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_MODULE=$(LOCAL_MODULE)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_CFLAGS=$(LOCAL_CFLAGS)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );

        if ( ! artifacts.isEmpty() )
        {
            makeFile.append( "LOCAL_PATH := $(call my-dir)\n" );
            for ( Artifact artifact : artifacts )
            {

                makeFile.append( "#\n" );
                makeFile.append( "# Group ID: " );
                makeFile.append( artifact.getGroupId() );
                makeFile.append( '\n' );
                makeFile.append( "# Artifact ID: " );
                makeFile.append( artifact.getArtifactId() );
                makeFile.append( '\n' );
                makeFile.append( "# Version: " );
                makeFile.append( artifact.getVersion() );
                makeFile.append( '\n' );
                makeFile.append( "include $(CLEAR_VARS)" );
                makeFile.append( '\n' );
                makeFile.append( "LOCAL_MODULE    := " );
                makeFile.append( artifact.getArtifactId() );
                makeFile.append( '\n' );
                makeFile.append( "LOCAL_SRC_FILES := " );
                makeFile.append( resolveRelativePath( outputDir, artifact.getFile() ) );
                makeFile.append( '\n' );
                if ( useHeaderArchives )
                {
                    try
                    {
                        Artifact harArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                artifact.getVersion(), artifact.getScope(), "har", artifact.getClassifier(),
                                artifact.getArtifactHandler() );
                        final Artifact resolvedHarArtifact = AetherHelper
                                .resolveArtifact( harArtifact, repoSystem, repoSession, projectRepos );

                        File includeDir = new File( System.getProperty( "java.io.tmpdir" ),
                                "android_maven_plugin_native_includes" + System.currentTimeMillis() + "_"
                                        + resolvedHarArtifact.getArtifactId() );
                        includeDir.deleteOnExit();
                        includeDirectories.add( includeDir );

                        JarHelper.unjar( new JarFile( resolvedHarArtifact.getFile() ), includeDir,
                                new JarHelper.UnjarListener()
                                {
                                    @Override
                                    public boolean include( JarEntry jarEntry )
                                    {
                                        return ! jarEntry.getName().startsWith( "META-INF" );
                                    }
                                } );

                        makeFile.append( "LOCAL_EXPORT_C_INCLUDES := " );
                        final String str = includeDir.getAbsolutePath();
                        makeFile.append( str );
                        makeFile.append( '\n' );
                    }
                    catch ( Exception e )
                    {
                        throw new MojoExecutionException(
                                "Error while resolving header archive file for: " + artifact.getArtifactId(), e );
                    }
                }
                if ( "a".equals( artifact.getType() ) )
                {
                    makeFile.append( "include $(PREBUILT_STATIC_LIBRARY)\n" );
                }
                else
                {
                    makeFile.append( "include $(PREBUILT_SHARED_LIBRARY)\n" );
                }
            }
        }
        return new MakefileHolder( includeDirectories, makeFile.toString() );
    }


    /**
     * Resolves the relative path of the specified artifact
     *
     * @param outputDirectory
     * @param file
     * @return
     */
    public static String resolveRelativePath( File outputDirectory, File file )
    {
        // FIXME: This should really examine the paths used and correct the directory accordingly

        final StringBuilder stringBuilder = new StringBuilder();

        String separator = File.separator;

        // If on Windows, the file separator will be \\ which is an invalid Java regexp - rewrite into something
        // more sensible (like 4 slashes) - fix for issue 264
        if ( separator.equals( "\\" ) )
        {
            separator = "\\\\";
        }

        final String[] split = outputDirectory.getAbsolutePath().split( separator );

        if ( split == null || split.length == 0 )
        {
            return file.getAbsolutePath();
        }

        //
        stringBuilder.append( ".." );
        for ( int i = 0; i < split.length - 1; i++ )
        {
            stringBuilder.append( File.separator );
            stringBuilder.append( ".." );
        }
        return stringBuilder.toString() + file.getAbsolutePath();
    }

    /**
     * Creates a list of artifacts suitable for use in the LOCAL_STATIC_LIBRARIES variable in an Android makefile
     *
     * @param resolvedStaticLibraryList
     * @param staticLibrary
     * @return
     */
    public static String createLibraryList( Set<Artifact> resolvedStaticLibraryList, boolean staticLibrary )
    {
        StringBuilder sb = new StringBuilder();

        for ( Iterator<Artifact> iterator = resolvedStaticLibraryList.iterator(); iterator.hasNext(); )
        {
            Artifact resolvedstaticLibraryArtifact = iterator.next();
            if ( staticLibrary && "a".equals( resolvedstaticLibraryArtifact.getType() ) )
            {
                sb.append( resolvedstaticLibraryArtifact.getArtifactId() );
            }
            if ( ! staticLibrary && "so".equals( resolvedstaticLibraryArtifact.getType() ) )
            {
                sb.append( resolvedstaticLibraryArtifact.getArtifactId() );
            }
            if ( iterator.hasNext() )
            {
                sb.append( " " );
            }
        }

        return sb.toString();
    }
}