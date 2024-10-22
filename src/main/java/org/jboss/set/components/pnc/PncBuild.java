package org.jboss.set.components.pnc;

import java.util.Objects;

public class PncBuild {

    private final PncBuild.Id id;
    private final String brewComponent;

    public PncBuild(Id id, String brewComponent) {
        this.id = id;
        this.brewComponent = brewComponent;
    }

    public PncBuild.Id getId() {
        return id;
    }

    public String getBrewComponent() {
        return brewComponent;
    }

    @Override
    public String toString() {
        return "PncBuild{" +
                "id=" + id +
                ", brewComponent='" + brewComponent + '\'' +
                '}';
    }

    public static class Id {
        private final String id;

        public Id(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "Id{" +
                    "id='" + id + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id id1 = (Id) o;
            return Objects.equals(id, id1.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
