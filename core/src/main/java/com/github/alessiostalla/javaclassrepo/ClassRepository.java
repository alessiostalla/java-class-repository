package com.github.alessiostalla.javaclassrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ClassRepository implements ClassProvider {

    private static final Logger logger = LoggerFactory.getLogger(ClassRepository.class);

    public final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
    public final List<ClassProvider> classProviders = new ArrayList<ClassProvider>();

    protected final Map<String, ClassCacheEntry> classCache = new HashMap<String, ClassCacheEntry>();

    public ClassRepository() {
        this(true);
    }

    public ClassRepository(boolean includeDefaultClassLoaders) {
        if(includeDefaultClassLoaders) {
            classLoaders.add(getClass().getClassLoader());
        }
    }

    public synchronized Class getClass(String className) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                logger.debug("Class " + className + " not found in classloader " + classLoader, e);
            }
        }
        long timestamp = System.currentTimeMillis();
        ClassCacheEntry classCacheEntry = classCache.get(className);
        if (classCacheEntry == null) {
            loadClasses(className, timestamp);
            classCacheEntry = classCache.get(className);
        } else if (classCacheEntry.shouldReload()) {
            classCacheEntry.reload(timestamp);
            classCacheEntry = classCache.get(className);
        }
        if(classCacheEntry != null) {
            return classCacheEntry.getLoadedClass();
        } else {
            throw new ClassNotFoundException(className);
        }
    }

    /* TODO getClasses() by resource needs a different classCache (by resource name rather than by class name)
    public synchronized Class[] getClasses(String path) throws ClassNotFoundException {
        Resource resource = getResource(path);
        long timestamp = System.currentTimeMillis();
        ClassCacheEntry classCacheEntry = classCache.get(className);
        if (classCacheEntry == null) {
            loadClasses(className, timestamp);
            classCacheEntry = classCache.get(className);
        } else if (classCacheEntry.shouldReload()) {
            classCacheEntry.reload(timestamp);
            classCacheEntry = classCache.get(className);
        }
        if(classCacheEntry != null) {
            return classCacheEntry.getLoadedClass();
        } else {
            throw new ClassNotFoundException(className);
        }
    }*/

    public synchronized ClassRepository withClassLoaders(ListOperation<ClassLoader> op) {
        op.execute(classLoaders);
        return this;
    }

    public synchronized ClassRepository withClassProviders(ListOperation<ClassProvider> op) {
        op.execute(classProviders);
        return this;
    }

    public synchronized ClassRepository withClassLoaders(final ClassLoader... classLoaders) {
        return withClassLoaders(new ListOperation<ClassLoader>() {
            @Override
            public void execute(List<ClassLoader> objects) {
                objects.addAll(Arrays.asList(classLoaders));
            }
        });
    }

    public synchronized ClassRepository withClassProviders(final ClassProvider...classProviders) {
        return withClassProviders(new ListOperation<ClassProvider>() {
            @Override
            public void execute(List<ClassProvider> objects) {
                objects.addAll(Arrays.asList(classProviders));
            }
        });
    }

    protected Collection<ClassCacheEntry> loadClasses(String className, long timestamp) throws ClassNotFoundException {
        Resource resource = getResourceForClass(className);
        return loadClasses(resource, timestamp);

    }

    protected Collection<ClassCacheEntry> loadClasses(Resource resource, long timestamp) throws ClassNotFoundException {
        //TODO record dependencies
        try {
            if (resource.isClass()) {
                Class[] classes = resource.loadClasses(this);
                List<ClassCacheEntry> entries = new ArrayList<ClassCacheEntry>(classes.length);
                for(Class theClass : classes) {
                    ClassCacheEntry entry = new ClassCacheEntry(theClass.getName(), theClass, timestamp, resource.getProvider());
                    entries.add(entry);
                    classCache.put(theClass.getName(), entry);
                }
                return entries;
            }
        } finally {
            try {
                resource.close();
            } catch (IOException e) {
                logger.warn("Could not close resource: " + resource, e);
            }
        }
        throw new ClassNotFoundException("No class was found in resource " + resource);
    }

    @Override
    public Resource getResourceForClass(String className) {
        for(ClassProvider provider : classProviders) {
            Resource resource = provider.getResourceForClass(className);
            if(resource.isClass()) {
                return resource;
            }
        }
        return new NonExistingResource(this);
    }

    @Override
    public Resource getResource(String path) {
        for(ClassProvider provider : classProviders) {
            Resource resource = provider.getResource(path);
            if(resource.exists()) {
                return resource;
            }
        }
        return new NonExistingResource(this);
    }

    protected class ClassCacheEntry {
        public final String className;
        public final Class loadedClass;
        public final long timestamp;
        public final ClassProvider provider;
        //TODO dependencies

        public ClassCacheEntry(String className, Class loadedClass, long timestamp, ClassProvider provider) {
            this.className = className;
            this.loadedClass = loadedClass;
            this.timestamp = timestamp;
            this.provider = provider;
        }

        public boolean shouldReload() {
            return provider.getResourceForClass(className).isNewerThan(timestamp);
        }

        public Collection<ClassCacheEntry> reload(long timestamp) throws ClassNotFoundException {
            Class[] newClasses = provider.getResourceForClass(className).loadClasses(ClassRepository.this);
            classCache.remove(className);
            //TODO remove other old classes (so inners that no longer exist get garbage-collected)
            List<ClassCacheEntry> newEntries = new ArrayList<ClassCacheEntry>(newClasses.length);
            for(Class newClass : newClasses) {
                ClassCacheEntry newEntry = new ClassCacheEntry(className, newClass, timestamp, provider);
                classCache.put(className, newEntry);
                newEntries.add(newEntry);
            }
            return newEntries;
        }

        public Class getLoadedClass() throws ClassNotFoundException {
            if(loadedClass != null) {
                return loadedClass;
            } else {
                throw new ClassNotFoundException("Class " + className + " was cached as not found");
            }
        }
    }

    public class ClassLoaderFacade extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> cls = ClassRepository.this.getClass(name);
            if(cls != null) {
                return cls;
            } else {
                throw new ClassNotFoundException(name);
            }
        }

        public Class defineClass(String name, byte[] code) {
            return super.defineClass(name, code, 0, code.length);
        }
    }

    public ClassLoaderFacade asClassLoader() {
        return new ClassLoaderFacade();
    }

}
