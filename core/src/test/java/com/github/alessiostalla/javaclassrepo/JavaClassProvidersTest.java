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
        FileObject fo = manager.resolveFile("res://");
        final CompiledJavaClassProvider classProvider = new CompiledJavaClassProvider(fo);
        testClassProvider(classProvider);
    }

    public void testClassProvider(ClassProvider classProvider) throws ClassNotFoundException {
        assertEquals(classProvider.getResource("foo").getName(), "/foo");
        assertFalse(classProvider.getResource("foo").exists());
        Resource resource = classProvider.getResourceForClass(getClass().getName());
        assertTrue(resource.exists());
        assertTrue(resource.isClass());

        ClassRepository classRepository = new ClassRepository().withClassProviders(classProvider);
        Class aClass = classRepository.getClass(getClass().getName());
        assertEquals(getClass(), aClass, "Classes should be the same as they share the classloader");

        classRepository = new ClassRepository(false).withClassProviders(classProvider);
        aClass = classRepository.getClass(getClass().getName());
        assertNotEquals(getClass(), aClass, "Classes should NOT be the same as we explicitly told the ClassRepository not to use any predefined classloader");
        classRepository.getClass(AnotherTopLevelClass.class.getName());
        classRepository.getClass(AnotherTopLevelClass.StaticInnerClass.class.getName());
        classRepository.getClass(AnotherTopLevelClass.InnerClass.class.getName());
    }

    @Test
    public void testSourceClassProvider() throws Exception {
        File sourceDir = new File(new File("").getAbsoluteFile(), "core/src/test/java");
        if(sourceDir.isDirectory()) {
            FileSystemManager manager = VFS.getManager();
            FileObject fo = manager.resolveFile(sourceDir.toURI().toString());
            SourceJavaClassProvider classProvider = new SourceJavaClassProvider(fo);
            ClassRepository classRepository = new ClassRepository(false).withClassProviders(classProvider);
            try {
                classRepository.getClass(AnotherTopLevelClass.class.getName());
                fail("Initially, AnotherTopLevelClass should not be found");
            } catch (ClassNotFoundException e) {
                //Ok
            }
            testClassProvider(classProvider);
        } else {
            fail("TODO load source from as many locations as possible");
        }
    }

}

class AnotherTopLevelClass {

    static class StaticInnerClass {}

    class InnerClass {}

}
