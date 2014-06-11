package com.github.alessiostalla.javaclassrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassRepository {

    private static final Logger logger = LoggerFactory.getLogger(ClassRepository.class);

    public final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
    public final List<ClassProvider> classProviders = new ArrayList<ClassProvider>();

    protected final Map<String, ClassCacheEntry> classCache = new HashMap<String, ClassCacheEntry>();

    public ClassRepository() {
        classLoaders.add(getClass().getClassLoader());
    }

    public synchronized Class getClass(String className) {
        for(ClassLoader classLoader : classLoaders) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                logger.debug("Class " + className + " not found in classloader " + classLoader, e);
            }
        }
        long timestamp = System.currentTimeMillis();
        ClassCacheEntry classCacheEntry = classCache.get(className);
        if(classCacheEntry == null) {
            classCacheEntry = loadClass(className, timestamp);
        } else if(classCacheEntry.shouldReload()) {
            classCacheEntry = classCacheEntry.reload(timestamp);
        }
        return classCacheEntry.loadedClass;
    }

    protected ClassCacheEntry loadClass(String className, long timestamp) {
        //TODO
        for(ClassProvider provider : classProviders) {
            Resource resource = provider.getResourceForClass(className);
            if(resource.isClass()) {
                Class theClass = resource.loadClass(this);
                return new ClassCacheEntry(className, theClass, timestamp, provider);
            }
        }
        return new ClassCacheEntry(className, null, timestamp, null); //TODO
    }

    protected class ClassCacheEntry {
        public final String className;
        public final Class loadedClass;
        public final long timestamp;
        public final ClassProvider provider;

        public ClassCacheEntry(String className, Class loadedClass, long timestamp, ClassProvider provider) {
            this.className = className;
            this.loadedClass = loadedClass;
            this.timestamp = timestamp;
            this.provider = provider;
        }

        public boolean shouldReload() {
            return provider.getResourceForClass(className).isNewerThan(timestamp);
        }

        public ClassCacheEntry reload(long timestamp) {
            Class newClass = provider.getResourceForClass(className).loadClass(ClassRepository.this);
            ClassCacheEntry newEntry = new ClassCacheEntry(className, newClass, timestamp, provider);
            classCache.put(className, newEntry);
            return newEntry;
        }
    }

    public ClassLoader asClassLoader() {
        return new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                Class<?> cls = ClassRepository.this.getClass(name);
                if(cls != null) {
                    return cls;
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
        };
    }

}
