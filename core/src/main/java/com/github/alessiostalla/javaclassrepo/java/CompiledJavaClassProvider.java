package com.github.alessiostalla.javaclassrepo.java;

import com.github.alessiostalla.javaclassrepo.ClassRepository;
import com.github.alessiostalla.javaclassrepo.vfs.VFSClassProvider;
import com.github.alessiostalla.javaclassrepo.vfs.VFSResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by alessio on 04/07/14.
 */
public class CompiledJavaClassProvider extends VFSClassProvider {

    public CompiledJavaClassProvider(FileSystem fileSystem) throws FileSystemException {
        super(fileSystem);
    }

    public CompiledJavaClassProvider(FileSystem fileSystem, FileObject root) {
        super(fileSystem, root);
    }

    @Override
    protected VFSResource getResource(FileObject fileObject, final String className) {
        return new VFSResource(this, fileObject) {
            @Override
            public boolean isClass() {
                return exists() && "class".equals(fileObject.getName().getExtension());
            }

            @Override
            public Class loadClass(ClassRepository repository) throws ClassNotFoundException {
                ClassLoader classLoader = new URLClassLoader(new URL[0], repository.asClassLoader()) {
                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        if(name.equals(className)) {
                            try {
                                byte[] buf = IOUtils.toByteArray(fileObject.getContent().getInputStream());
                                return defineClass(name, buf, 0, buf.length);
                            } catch (IOException e) {
                                throw new ClassNotFoundException(className, e);
                            }
                        } else {
                            return super.loadClass(name);
                        }
                    }
                };
                return classLoader.loadClass(className);
            }
        };
    }

    @Override
    protected String translateToPath(String className) {
        return className.replace(".", "/").concat(".class");
    }
}
