package com.github.alessiostalla.javaclassrepo.vfs;

import com.github.alessiostalla.javaclassrepo.ClassProvider;
import com.github.alessiostalla.javaclassrepo.ClassRepository;
import com.github.alessiostalla.javaclassrepo.Resource;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alessio on 02/07/14.
 */
public abstract class VFSResource implements Resource {

    protected final VFSClassProvider provider;
    protected final FileObject fileObject;

    public VFSResource(VFSClassProvider provider, FileObject fileObject) {
        this.provider = provider;
        this.fileObject = fileObject;
    }

    @Override
    public boolean exists() {
        try {
            return fileObject.exists();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isNewerThan(long timestamp) {
        try {
            return fileObject.exists() && fileObject.getContent().getLastModifiedTime() > timestamp;
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return fileObject.getContent().getInputStream();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            fileObject.close();
        } catch (FileSystemException e) {
            throw new IOException("Could not close file object: " + fileObject.getName().getFriendlyURI(), e);
        }
    }

    @Override
    public ClassProvider getProvider() {
        return provider;
    }

    @Override
    public String getName() {
        try {
            return "/" + provider.getRoot().getName().getRelativeName(fileObject.getName());
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " (file object: " + fileObject.getName().getFriendlyURI() + ")";
    }
}
