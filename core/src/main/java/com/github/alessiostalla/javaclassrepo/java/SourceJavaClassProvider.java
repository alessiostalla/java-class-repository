package com.github.alessiostalla.javaclassrepo.java;

import com.github.alessiostalla.javaclassrepo.ClassRepository;
import com.github.alessiostalla.javaclassrepo.vfs.VFSClassProvider;
import com.github.alessiostalla.javaclassrepo.vfs.VFSResource;
import org.apache.commons.vfs2.FileObject;

import javax.tools.*;
import java.io.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by alessio on 04/07/14.
 */
public class SourceJavaClassProvider extends VFSClassProvider {

    public SourceJavaClassProvider(FileObject root) {
        super(root);
    }

    @Override
    protected VFSResource getResource(FileObject fileObject) {
        return new VFSResource(this, fileObject) {
            @Override
            public boolean isClass() {
                return exists() && "java".equals(fileObject.getName().getExtension());
            }

            @Override
            public Class[] loadClasses(ClassRepository repository) throws ClassNotFoundException {
                String className = getName().substring(0, getName().length() - ".java".length()).replace("/", ".");
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                VFSFileManager fileManager = new VFSFileManager<StandardJavaFileManager>(root, compiler.getStandardFileManager(null, null, null));
                try {
                    JavaFileObject javaFile = fileManager.getJavaFileForInput(StandardLocation.locationFor(fileObject.getName().getPath()), className, JavaFileObject.Kind.SOURCE);
                    compiler.getTask(null, fileManager, null, null, null, Arrays.asList(javaFile)).call();
                    Map<String, ByteArrayOutputStream> classes = fileManager.getClasses();
                    ClassRepository.ClassLoaderFacade classLoaderFacade = repository.asClassLoader();
                    Class[] result = new Class[classes.size()];
                    int i = 0;
                    for(Map.Entry<String, ByteArrayOutputStream> entry : classes.entrySet()) {
                        result[i++] = classLoaderFacade.defineClass(entry.getKey(), entry.getValue().toByteArray());
                    }
                    return result;
                } catch (IOException e) {
                    throw new ClassNotFoundException(className, e);
                } finally {
                    try {
                        fileManager.close();
                    } catch (IOException e) {
                        e.printStackTrace(); //TODO log
                    }
                }

            }
        };
    }

    @Override
    protected String translateToPath(String className) {
        return className.replace(".", "/").concat(".java");
    }
}
