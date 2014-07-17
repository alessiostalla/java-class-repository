package com.github.alessiostalla.javaclassrepo;

import com.github.alessiostalla.javaclassrepo.java.CompiledJavaClassProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class CompiledJavaClassProviderTest {

    @Test
    public void test() throws Exception {
        FileSystemManager manager = VFS.getManager();
        FileObject fo = manager.resolveFile("ram://test/");
        FileSystem fileSystem = fo.getFileSystem();
        final CompiledJavaClassProvider compiledJavaClassProvider = new CompiledJavaClassProvider(fileSystem);
        assertEquals(compiledJavaClassProvider.getResource("foo").getName(), "/foo");
        assertFalse(compiledJavaClassProvider.getResource("foo").exists());
        //Let's create some classes...
        Resource resource = compiledJavaClassProvider.getResourceForClass(getClass().getName());
        FileObject fileObject = fileSystem.resolveFile(resource.getName());
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

}
