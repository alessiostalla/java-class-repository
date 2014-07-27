package com.github.alessiostalla.javaclassrepo;

import com.github.alessiostalla.javaclassrepo.java.CompiledJavaClassProvider;
import com.github.alessiostalla.javaclassrepo.java.SourceJavaClassProvider;
import com.github.alessiostalla.javaclassrepo.vfs.VFSResource;
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
        //Additional tests
        ClassRepository classRepository = new ClassRepository(false).withClassProviders(classProvider);
        Class staticInnerSubclass = classRepository.getClass(AnotherTopLevelClass.StaticInnerSubclass.class.getName());
        VFSResource resource = classProvider.getResourceForClass(AnotherTopLevelClass.StaticInnerClass.class.getName());
        long refTime = System.currentTimeMillis();
        assertFalse(resource.isNewerThan(refTime));
        resource.getFileObject().getContent().setLastModifiedTime(refTime + 1000);
        assertTrue(resource.isNewerThan(refTime));
        Class reloadedSubclass = classRepository.getClass(AnotherTopLevelClass.StaticInnerSubclass.class.getName());
        assertFalse(staticInnerSubclass.equals(reloadedSubclass), "The class should have been reloaded due to changed dependencies");
        while (resource.isNewerThan(System.currentTimeMillis())) {
            Thread.sleep(500);
        }
        resource.getFileObject().getContent().setLastModifiedTime(refTime); //Why is this needed? Investigate
        assertFalse(resource.isNewerThan(System.currentTimeMillis()));
        assertTrue(reloadedSubclass.equals(classRepository.getClass(AnotherTopLevelClass.StaticInnerSubclass.class.getName())), "Now, no further reloading should happen");
    }

    public void testClassProvider(ClassProvider classProvider) throws ClassNotFoundException {
        assertEquals(classProvider.getResource("foo").getName(), "foo");
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
        //If the order of the following two instructions is swapped, CompiledJavaClassProvider fails because it uses
        //two different classloaders, so the the superclass is not accessible to the subclass (package-protected means
        //same package AND same classloader). This is a known limitation.
        Class staticInnerSubclass = classRepository.getClass(AnotherTopLevelClass.StaticInnerSubclass.class.getName());
        Class staticInner = classRepository.getClass(AnotherTopLevelClass.StaticInnerClass.class.getName());
        classRepository.getClass(AnotherTopLevelClass.InnerClass.class.getName());
        assertEquals(staticInnerSubclass.getSuperclass(), staticInner);
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
            fail("TODO load source from as many locations as possible (sourceDir: " + sourceDir + ")");
        }
    }

    @Test
    public void testBoth() throws Exception {
        FileSystemManager manager = VFS.getManager();
        FileObject fo = manager.resolveFile("res://");
        CompiledJavaClassProvider compiled = new CompiledJavaClassProvider(fo);
        SourceJavaClassProvider source = new SourceJavaClassProvider(fo);
        //Additional tests
        ClassRepository classRepository = new ClassRepository(false).withClassProviders(source, compiled);
        classRepository.getClass("com.github.alessiostalla.javaclassrepo.source.SourceSub");
        classRepository = new ClassRepository(false).withClassProviders(compiled, source);
        classRepository.getClass("com.github.alessiostalla.javaclassrepo.source.SourceSub");
    }

}

class AnotherTopLevelClass {

    public static class StaticInnerClass {} //Must be public because package-protected breaks with multiple classloaders

    class InnerClass {}

    static class StaticInnerSubclass extends StaticInnerClass {}

}
