package org.jboss.set.components.pnc;

import org.wildfly.channel.ArtifactCoordinate;

public class PncArtifact {

    private final Id id;
    private final ArtifactCoordinate coordinate;
    private final boolean imported;

    public PncArtifact(Id id, ArtifactCoordinate coordinate, boolean imported) {
        this.id = id;
        this.coordinate = coordinate;
        this.imported = imported;
    }

    public Id getId() {
        return id;
    }

    public boolean isImported() {
        return imported;
    }

    public ArtifactCoordinate getCoordinate() {
        return coordinate;
    }


    @Override
    public String toString() {
        return "PncArtifact{" +
                "id=" + id +
                ", coordinate=" + coordinate +
                ", imported=" + imported +
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
    }
}
