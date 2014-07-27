package com.github.alessiostalla.javaclassrepo.java;

import com.github.alessiostalla.javaclassrepo.ClassRepository;
import com.github.alessiostalla.javaclassrepo.vfs.VFSClassProvider;
import com.github.alessiostalla.javaclassrepo.vfs.VFSResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by alessio on 04/07/14.
 */
public class CompiledJavaClassProvider extends VFSClassProvider {

    public CompiledJavaClassProvider(FileObject root) {
        super(root);
    }

    private String className;
    private ClassLoader classLoader;

    @Override
    protected VFSResource getResource(FileObject fileObject) {
        return new VFSResource(this, fileObject) {
            @Override
            public boolean isClass() {
                return exists() && "class".equals(fileObject.getName().getExtension());
            }

            @Override
            public Class[] loadClasses(final ClassRepository repository) throws ClassNotFoundException {
                String className = getName().substring(0, getName().length() - ".class".length()).replace("/", ".");
                CompiledJavaClassProvider.this.className = className;
                if(classLoader == null) {
                    classLoader = createInnerLoader(repository);
                }
                Class<?> c = classLoader.loadClass(className);
                classLoader = null;
                CompiledJavaClassProvider.this.className = null;
                return new Class[] {c};
            }
        };
    }

    protected URLClassLoader createInnerLoader(final ClassRepository repository) {
        return new URLClassLoader(new URL[0]) {
            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if(name.equals(CompiledJavaClassProvider.this.className)) {
                    VFSResource resource = getResourceForClass(name);
                    try {
                        byte[] buf = IOUtils.toByteArray(resource.getInputStream());
                        Class<?> c = defineClass(name, buf, 0, buf.length);
                        if(resolve) {
                            resolveClass(c);
                        }
                        return c;
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    } finally {
                        IOUtils.closeQuietly(resource);
                    }
                } else try {
                    return repository.getClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                }
            }
        };
    }

    @Override
    protected String translateToPath(String className) {
        return className.replace(".", "/").concat(".class");
    }
}
