package org.jboss.set.components;

import org.wildfly.channel.ArtifactCoordinate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Violation {

    private String componentName;
    private Map<String, List<ArtifactCoordinate>> artifactsByVersion;

    public Violation(String componentName, Map<String, List<ArtifactCoordinate>> artifactsByVersion) {
        this.componentName = componentName;
        this.artifactsByVersion = artifactsByVersion;
    }

    public String getComponentName() {
        return componentName;
    }

    public Map<String, List<ArtifactCoordinate>> getArtifactsByVersion() {
        return artifactsByVersion;
    }

    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ERROR] Component ").append(componentName).append(" has multiple versions").append(System.lineSeparator());
        for (String version : artifactsByVersion.keySet()) {
            sb.append("        ").append(version).append(System.lineSeparator());
            for (ArtifactCoordinate artifactCoordinate : artifactsByVersion.get(version)) {
                sb.append("          * ").append(artifactCoordinate.getGroupId()).append(":").append(artifactCoordinate.getArtifactId()).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Violation{" +
                "componentName='" + componentName + '\'' +
                ", artifactsByVersion=" + artifactsByVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Violation violation = (Violation) o;
        return Objects.equals(componentName, violation.componentName) && Objects.equals(artifactsByVersion, violation.artifactsByVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, artifactsByVersion);
    }
}
