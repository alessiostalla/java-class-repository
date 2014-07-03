package com.github.alessiostalla.javaclassrepo.vfs;

import com.github.alessiostalla.javaclassrepo.ClassProvider;
import com.github.alessiostalla.javaclassrepo.Resource;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by alessio on 02/07/14.
 */
public abstract class VFSClassProvider implements ClassProvider {

    private static final Logger logger = LoggerFactory.getLogger(VFSClassProvider.class);

    protected final FileSystem fileSystem;
    protected final FileObject root;

    public VFSClassProvider(FileSystem fileSystem, FileObject root) {
        this.fileSystem = fileSystem;
        this.root = root;
    }

    public VFSClassProvider(FileSystem fileSystem) throws FileSystemException {
        this(fileSystem, fileSystem.getRoot());
    }

    @Override
    public Resource getResourceForClass(String className) {
        String path = translateToPath(className);
        return getResource(path);
    }

    @Override
    public Resource getResource(String path) {
        FileObject fileObject;
        try {
            fileObject = root.resolveFile(path);
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return getResource(fileObject);
    }

    protected abstract VFSResource getResource(FileObject fileObject);

    protected abstract String translateToPath(String className);
}
