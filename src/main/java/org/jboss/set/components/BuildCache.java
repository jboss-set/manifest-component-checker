package org.jboss.set.components;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.jboss.set.components.pnc.PncBuild;
import org.jboss.set.components.pnc.PncComponent;

class BuildCache {
    final Map<Key, Entry> cache = new ConcurrentHashMap<>();

    static Key toKey(String... parts) {
        return new Key(parts);
    }

    void cache(Key key, PncComponent componentName, PncBuild.Id buildId) {
        cache.put(key, new Entry(componentName, buildId));
    }

    Entry get(Key key) {
        return cache.get(key);
    }

    boolean contains(Key key) {
        return cache.containsKey(key);
    }

    static class Key {
        private final String key;

        Key(String... parts) {
            this.key = StringUtils.join(parts, ":");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key that = (Key) o;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }


    static class Entry {
        private final PncBuild.Id buildId;
        private final PncComponent componentName;

        public Entry(PncComponent componentName, PncBuild.Id buildId) {
            this.buildId = buildId;
            this.componentName = componentName;
        }

        public PncBuild.Id getBuildId() {
            return buildId;
        }

        public PncComponent getComponentName() {
            return componentName;
        }
    }
}
