package org.jboss.set.components.pnc;

import org.wildfly.channel.ArtifactCoordinate;

import java.util.List;

public interface PncManager {

    PncArtifact getArtifact(ArtifactCoordinate coordinate);

    PncBuild getBuildIdContainingArtifact(PncArtifact.Id artifactId);

    List<PncArtifact> getArtifactsInBuild(PncBuild.Id buildId);
}
