package com.github.alessiostalla.javaclassrepo.java;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.FileObject;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by alessio on 17/07/14.
 */
public class VFSFileManager<M extends JavaFileManager> extends ForwardingJavaFileManager<M> {

    protected final FileObject root;
    protected final Queue<FileObject> openFiles = new ConcurrentLinkedQueue<FileObject>();
    protected final Map<String, ByteArrayOutputStream> compiledClasses = new LinkedHashMap<String, ByteArrayOutputStream>();

    /**
     * Creates a new instance of ForwardingJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     */
    public VFSFileManager(FileObject root, M fileManager) {
        super(fileManager);
        this.root = root;
    }

    @Override
    public void close() throws IOException {
        for(FileObject file : openFiles) {
            file.close();
        }
        openFiles.clear();
        super.close();
    }

    @Override
    public javax.tools.FileObject getFileForOutput(Location location, String packageName, String relativeName, javax.tools.FileObject sibling) throws IOException {
        FileObject file = root.resolveFile(location.getName());
        openFiles.add(file);
        return new VFSFileObject(file);
    }

    @Override
    public javax.tools.FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        FileObject file = root.resolveFile(location.getName());
        openFiles.add(file);
        return new VFSFileObject(file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, final String className, JavaFileObject.Kind kind, javax.tools.FileObject sibling) throws IOException {
        return new SimpleJavaFileObject(URI.create(className), kind) {
            public OutputStream openOutputStream() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                compiledClasses.put(className, baos);
                return baos;
            }
        };
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
        if (location == StandardLocation.CLASS_OUTPUT && compiledClasses.containsKey(className) && kind == JavaFileObject.Kind.CLASS) {
            final byte[] bytes = compiledClasses.get(className).toByteArray();
            return new SimpleJavaFileObject(URI.create(className), kind) {
                public InputStream openInputStream() {
                    return new ByteArrayInputStream(bytes);
                }
            };
        } else {
            FileObject file = root.resolveFile(location.getName());
            openFiles.add(file);
            return new VFSJavaFileObject(file, kind);
        }
    }

    public Map<String, ByteArrayOutputStream> getClasses() {
        return compiledClasses;
    }

    public static class VFSFileObject implements javax.tools.FileObject {

        protected final FileObject impl;

        public VFSFileObject(FileObject impl) {
            this.impl = impl;
        }

        @Override
        public URI toUri() {
            try {
                return impl.getURL().toURI();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String getName() {
            return impl.getName().getPath();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return impl.getContent().getInputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return impl.getContent().getOutputStream();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new InputStreamReader(openInputStream()); //TODO encoding
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return IOUtils.toString(openInputStream()); //TODO encoding
        }

        @Override
        public Writer openWriter() throws IOException {
            return new OutputStreamWriter(openOutputStream()); //TODO encoding
        }

        @Override
        public long getLastModified() {
            try {
                return impl.getContent().getLastModifiedTime();
            } catch (FileSystemException e) {
                return 0;
            }
        }

        @Override
        public boolean delete() {
            try {
                return impl.delete();
            } catch (FileSystemException e) {
                return false;
            }
        }
    }

    public static class VFSJavaFileObject extends VFSFileObject implements JavaFileObject {

        protected final Kind kind;

        public VFSJavaFileObject(FileObject impl, Kind kind) {
            super(impl);
            this.kind = kind;
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            String baseName = simpleName + kind.extension;
            return kind.equals(getKind())
                    && (baseName.equals(getName())
                    || getName().endsWith("/" + baseName));
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }
    }
}
