package org.jboss.set.components;

import org.wildfly.channel.ArtifactCoordinate;

import java.util.List;

public class Warning {
    private String message;
    private List<ArtifactCoordinate> artifactCoordinates;

    public Warning(String message, List<ArtifactCoordinate> artifactCoordinate) {
        this.message = message;
        this.artifactCoordinates = artifactCoordinate;
    }

    public String getMessage() {
        return message;
    }

    public List<ArtifactCoordinate> getArtifactCoordinates() {
        return artifactCoordinates;
    }

    public String print() {
        final StringBuilder sb = new StringBuilder();
        sb.append(message).append(System.lineSeparator());

        artifactCoordinates.forEach(ac->sb.append(String.format("          * %s:%s:%s%n", ac.getGroupId(),
                ac.getArtifactId(), ac.getVersion())));

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Warning{" +
                "message='" + message + '\'' +
                ", artifactCoordinates=" + artifactCoordinates +
                '}';
    }
}
