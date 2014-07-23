package com.github.alessiostalla.javaclassrepo;

import com.github.alessiostalla.javaclassrepo.java.CompiledJavaClassProvider;
import com.github.alessiostalla.javaclassrepo.java.SourceJavaClassProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

public class JavaClassProvidersTest {

    @Test
    public void testCompiledJavaClassProvider() throws Exception {
        FileSystemManager manager = VFS.getManager();
        FileObject fo = manager.resolveFile("ram://test/");
        final CompiledJavaClassProvider compiledJavaClassProvider = new CompiledJavaClassProvider(fo);
        assertEquals(compiledJavaClassProvider.getResource("foo").getName(), "/foo");
        assertFalse(compiledJavaClassProvider.getResource("foo").exists());
        //Let's create some classes...
        Resource resource = compiledJavaClassProvider.getResourceForClass(getClass().getName());
        FileObject fileObject = fo.resolveFile("/test" + resource.getName());
        fileObject.createFile();
        IOUtils.copy(getClass().getResourceAsStream(resource.getName()), fileObject.getContent().getOutputStream());
        resource.close();

        ClassRepository classRepository = new ClassRepository().withClassProviders(compiledJavaClassProvider);
        Class aClass = classRepository.getClass(getClass().getName());
        assertEquals(getClass(), aClass); //Shares the same classloader

        classRepository = new ClassRepository(false).withClassProviders(compiledJavaClassProvider);
        aClass = classRepository.getClass(getClass().getName());
        assertNotEquals(getClass(), aClass); //Does not share the same classloader
    }

    @Test
    public void testSourceClassProvider() throws Exception {
        File sourceDir = new File(new File("").getAbsoluteFile(), "core/src/test/java");
        if(sourceDir.isDirectory()) {
            FileSystemManager manager = VFS.getManager();
            FileObject fo = manager.resolveFile(sourceDir.toURI().toString());
            final SourceJavaClassProvider sourceJavaClassProvider = new SourceJavaClassProvider(fo);
            assertEquals(sourceJavaClassProvider.getResource("foo").getName(), "/foo");
            assertFalse(sourceJavaClassProvider.getResource("foo").exists());
            Resource resource = sourceJavaClassProvider.getResourceForClass(getClass().getName());
            assertTrue(resource.exists());
            assertTrue(resource.isClass());
            ClassRepository classRepository = new ClassRepository(false).withClassProviders(sourceJavaClassProvider);
            Class aClass = classRepository.getClass(getClass().getName());
            assertNotEquals(aClass, getClass());
            assertEquals(aClass.getName(), getClass().getName());
        } else {
            fail("TODO load source from as many locations as possible");
        }
    }

}
