package org.jboss.set.components.pnc;

import org.apache.commons.lang3.StringUtils;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.response.ArtifactInfo;
import org.wildfly.channel.ArtifactCoordinate;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PncManagerImpl implements PncManager {

    private final URL pncUrl;
    private final Configuration configuration;

    public PncManagerImpl(URL pncUrl) {
        this.pncUrl = pncUrl;
        this.configuration = Configuration.builder()
                .host(pncUrl.getHost())
                .port(pncUrl.getPort())
                .protocol(pncUrl.getProtocol())
                .build();
    }

    @Override
    public PncArtifact getArtifact(ArtifactCoordinate coordinate) {
        try (var artifactClient = new ArtifactClient(configuration)) {
            final var allFiltered = artifactClient.getAllFiltered(coordinate.getGroupId() + ":" + coordinate.getArtifactId() + ":*:" + coordinate.getVersion(),
                    null, null, null);

//            allFiltered.forEach(System.out::println);
            // TODO: check that all artifacts are coming from the same build
            final ArtifactInfo artifact = allFiltered.iterator().next();
            final var artifactInfo = artifactClient.getSpecific(artifact.getId());
            return new PncArtifact(new PncArtifact.Id(artifact.getId()), parseIdentifier(artifact.getIdentifier()), StringUtils.isNotEmpty(artifactInfo.getOriginUrl()));
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArtifactCoordinate parseIdentifier(String identifier) {
        final var parts = identifier.split(":");
        if (parts.length < 4 || parts.length > 5) {
            throw new RuntimeException("Unable to parse identifier " + identifier);
        }
        final var classifier = parts.length > 4 ? parts[4] : null;
        return new ArtifactCoordinate(parts[0], parts[1], parts[2], classifier, parts[3]);
    }

    @Override
    public PncBuild getBuildIdContainingArtifact(PncArtifact.Id artifact) {
        final String buildId;
        try (var artifactClient = new ArtifactClient(configuration)) {
            final var artifactInfo = artifactClient.getSpecific(artifact.getId());
            if (artifactInfo.getBuild() == null) {
                return null;
            }

            buildId = artifactInfo.getBuild().getId();
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }

        try (var buildClient = new BuildClient(configuration)) {
            var brewComponent = buildClient.getSpecific(buildId).getAttributes().get("BREW_BUILD_NAME");

            return new PncBuild(new PncBuild.Id(buildId), brewComponent);
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PncArtifact> getArtifactsInBuild(PncBuild.Id buildId) {
        try (var buildClient = new BuildClient(configuration)) {
            final var artifactList = buildClient.getBuiltArtifacts(buildId.getId());

            final var results = new ArrayList<PncArtifact>();
            for (Artifact artifact : artifactList) {
                results.add(new PncArtifact(new PncArtifact.Id(artifact.getId()), parseIdentifier(artifact.getIdentifier()), false));
            }
            return results;
        } catch (RemoteResourceException e) {
            throw new RuntimeException(e);
        }
    }
}
