package org.jboss.set.components;

import org.apache.commons.lang3.StringUtils;
import org.jboss.set.components.pnc.PncArtifact;
import org.jboss.set.components.pnc.PncBuild;
import org.jboss.set.components.pnc.PncManager;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.Stream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ManifestVerifier {
    final PncManager pncManager;

    public ManifestVerifier(PncManager pncManager) {
        this.pncManager = pncManager;
    }

    public VerificationResult verifyComponents(URL manifestURL) throws MalformedURLException {
        final ChannelManifest manifest = ChannelManifestMapper.from(manifestURL);
        final Collection<Stream> streams = manifest.getStreams();

        // GA:V - map of artifacts and all the versions found in included PNC build for this artifact
        final ConcurrentMap<String, Collection<ArtifactCoordinate>> components = new ConcurrentHashMap<>();
        // GA:PNC_BUILD_ID - map of artifacts and all the PNC builds for this artifact
        final ConcurrentMap<String, Collection<String>> builds = new ConcurrentHashMap<>();
        final Collection<ArtifactCoordinate> imported = new ConcurrentLinkedQueue<>();
        final Collection<ArtifactCoordinate> ungrouped = new ArrayList<>();

        // cache GA:Component
        final Map<String, BuildInfo> cache = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger(0);
        streams.parallelStream().forEach((stream)-> {
            final String cacheKey = toKey(stream.getGroupId(), stream.getArtifactId(), stream.getVersion());
            final ArtifactCoordinate artifactCoordinate = stream2Coord(stream);
            System.out.printf("Resolving [%d/%d]: %s%n", counter.getAndIncrement(), streams.size(), artifactCoordinate);
            if (cache.containsKey(cacheKey)) {
                // we resolved that artifact as part of one of earlier builds, let's just add this
                final BuildInfo buildInfo = cache.get(cacheKey);
                components.get(buildInfo.componentName).add(artifactCoordinate);
                builds.get(buildInfo.componentName).add(buildInfo.buildId);
            } else {

                final PncArtifact artifact = pncManager.getArtifact(artifactCoordinate);

                if (artifact.isImported()) {
                    imported.add(artifact.getCoordinate());
                    return;
                }

                final PncBuild build = pncManager.getBuildIdContainingArtifact(artifact.getId());

                if (build == null) {
                    ungrouped.add(artifact.getCoordinate());
                    return;
                }

                components.putIfAbsent(build.getBrewComponent(), Collections.synchronizedCollection(new HashSet<>()));

                components.get(build.getBrewComponent()).add(artifact.getCoordinate());

                builds.putIfAbsent(build.getBrewComponent(), Collections.synchronizedCollection(new HashSet<>()));

                builds.get(build.getBrewComponent()).add(build.getId().getId());

                final List<PncArtifact> componentArtifacts = pncManager.getArtifactsInBuild(build.getId());

                for (PncArtifact componentArtifact : componentArtifacts) {
                    final String compKey = toKey(componentArtifact.getCoordinate().getGroupId(),
                            componentArtifact.getCoordinate().getArtifactId(), componentArtifact.getCoordinate().getVersion());
                    cache.putIfAbsent(compKey, new BuildInfo(build.getBrewComponent(), build.getId()));
                }
            }
        });

        final VerificationResult res = new VerificationResult();

        for (String componentName : components.keySet()) {
            final Collection<ArtifactCoordinate> artifactCoordinates = components.get(componentName);
            final Map<String, List<ArtifactCoordinate>> artifactsByVersion = artifactCoordinates.stream()
                    .collect(Collectors.groupingBy(ArtifactCoordinate::getVersion));

            final Collection<String> buildIDs = builds.get(componentName);
            if (buildIDs.size() > 1) {
                res.addViolation(new Violation(componentName, artifactsByVersion));
            } else if (artifactsByVersion.size() > 1) {
                res.addWarning(new Warning("[WARN] Different versions of artifact from the same build:", new ArrayList<>(artifactCoordinates)));
            }
        }


        if (!imported.isEmpty()) {
            res.addWarning(new Warning("[WARN] Ignored imported artifacts:", new ArrayList<>(imported)));
        }

        if (!ungrouped.isEmpty()) {
            res.addWarning(new Warning("[WARN] Unable to determine components of:", new ArrayList<>(imported)));
        }

        return res;
    }

    private static ArtifactCoordinate stream2Coord(Stream stream) {
        final ArtifactCoordinate artifactCoordinate = new ArtifactCoordinate(
                stream.getGroupId(),
                stream.getArtifactId(),
                null,
                null,
                stream.getVersion());
        return artifactCoordinate;
    }

    private static String toKey(String... parts) {
        return StringUtils.join(parts, ":");
    }

    static class BuildInfo {
        private final String buildId;
        private final String componentName;

        public BuildInfo(String componentName, PncBuild.Id buildId) {
            this.buildId = buildId.getId();
            this.componentName = componentName;
        }
    }

}
