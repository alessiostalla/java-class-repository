package com.github.alessiostalla.javaclassrepo.groovy;

import com.github.alessiostalla.javaclassrepo.ClassRepository;
import com.github.alessiostalla.javaclassrepo.vfs.VFSClassProvider;
import com.github.alessiostalla.javaclassrepo.vfs.VFSResource;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by alessio on 04/07/14.
 */
public class GroovyClassProvider extends VFSClassProvider {

    public GroovyClassProvider(FileObject root) {
        super(root);
    }

    @Override
    protected VFSResource getResource(FileObject fileObject) {
        return new VFSResource(this, fileObject) {
            @Override
            public boolean isClass() {
                return exists() && "groovy".equals(fileObject.getName().getExtension());
            }

            @Override
            public Class[] loadClasses(final ClassRepository repository) throws ClassNotFoundException {
                String className = getName().substring(0, getName().length() - ".groovy".length()).replace("/", ".");
                GroovyClassLoader classLoader = createInnerLoader(repository);
                GroovyCodeSource codeSource;
                try {
                    codeSource = new GroovyCodeSource(IOUtils.toString(getInputStream()), className, root.getURL().toString());
                } catch (IOException e) {
                    throw new ClassNotFoundException(className, e);
                }
                codeSource.setCachable(false);
                Class<?> c = classLoader.parseClass(codeSource);
                return new Class[] {c};
            }
        };
    }

    protected GroovyClassLoader createInnerLoader(final ClassRepository repository) {
        return new GroovyClassLoader(repository.asClassLoader());
        /*return new URLClassLoader(new URL[0]) {
            @Override
            public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if(name.equals(GroovyClassProvider.this.className)) {
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
        };*/
    }

    @Override
    protected String translateToPath(String className) {
        return className.replace(".", "/").concat(".groovy");
    }
}
