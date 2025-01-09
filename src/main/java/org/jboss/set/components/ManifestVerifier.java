package org.jboss.set.components;

import org.jboss.set.components.pnc.PncArtifact;
import org.jboss.set.components.pnc.PncBuild;
import org.jboss.set.components.pnc.PncComponent;
import org.jboss.set.components.pnc.PncManager;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.Stream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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

        final BuildRecorder recorder = new BuildRecorder();

        final Collection<ArtifactCoordinate> imported = new ConcurrentLinkedQueue<>();
        final List<ArtifactCoordinate> missingArtifacts = new ArrayList<>();
        final Collection<ArtifactCoordinate> ungrouped = new ArrayList<>();

        // record all artifacts from resolved builds, so that we don't need to resolve them twice
        final BuildCache cache = new BuildCache();

        final AtomicInteger counter = new AtomicInteger(0);
        streams.parallelStream().forEach((stream)-> {
            final BuildCache.Key cacheKey = BuildCache.toKey(stream.getGroupId(), stream.getArtifactId(), stream.getVersion());
            final ArtifactCoordinate artifactCoordinate = stream2Coord(stream);
            System.out.printf("Resolving [%d/%d]: %s%n", counter.getAndIncrement(), streams.size(), artifactCoordinate);
            if (cache.contains(cacheKey)) {
                // we resolved that artifact as part of one of earlier builds, let's just add this
                final BuildCache.Entry buildCacheEntry = cache.get(cacheKey);

                recorder.record(buildCacheEntry.getBuildId(), buildCacheEntry.getComponentName(), artifactCoordinate);
            } else {

                final PncArtifact artifact = pncManager.getArtifact(artifactCoordinate);

                if (artifact == null) {
                    missingArtifacts.add(artifactCoordinate);
                    return;
                }

                if (artifact.isImported()) {
                    imported.add(artifact.getCoordinate());
                    return;
                }

                final PncBuild build = pncManager.getBuildIdContainingArtifact(artifact.getId());

                if (build == null) {
                    ungrouped.add(artifact.getCoordinate());
                    return;
                }

                recorder.record(build.getId(), build.getBrewComponent(), artifactCoordinate);

                final List<PncArtifact> componentArtifacts = pncManager.getArtifactsInBuild(build.getId());

                for (PncArtifact componentArtifact : componentArtifacts) {
                    final BuildCache.Key compKey = BuildCache.toKey(componentArtifact.getCoordinate().getGroupId(),
                            componentArtifact.getCoordinate().getArtifactId(), componentArtifact.getCoordinate().getVersion());
                    cache.cache(compKey, build.getBrewComponent(), build.getId());
                }
            }
        });

        final VerificationResult res = new VerificationResult();

        for (PncComponent component : recorder.recordedComponents()) {

            final Collection<BuildRecord> componentBuild = recorder.recordedBuildsOf(component);
            final Collection<ArtifactCoordinate> artifactCoordinates = componentBuild.stream()
                    .flatMap(r->r.includedArtifacts.stream())
                    .collect(Collectors.toList());
            final Map<String, List<ArtifactCoordinate>> artifactsByVersion = artifactCoordinates.stream()
                    .collect(Collectors.groupingBy(ArtifactCoordinate::getVersion));

            if (componentBuild.size() > 1) {
                res.addViolation(new Violation(component.getName(), artifactsByVersion));
            } else if (artifactsByVersion.size() > 1) {
                res.addWarning(new Warning("[WARN] Different versions of artifact from the same build:", new ArrayList<>(artifactCoordinates)));
            }
        }

        if (!missingArtifacts.isEmpty()) {
            res.addWarning(new Warning("[WARN] Artifacts not build in PNC:", missingArtifacts));
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
        return new ArtifactCoordinate(
                stream.getGroupId(),
                stream.getArtifactId(),
                null,
                null,
                stream.getVersion());
    }

    static class BuildRecorder {
        Map<PncComponent, Map<PncBuild.Id, Collection<ArtifactCoordinate>>> buildsByComponent = new ConcurrentHashMap<>();
        void record(PncBuild.Id buildId, PncComponent component, ArtifactCoordinate artifactCoordinate) {
            Map<PncBuild.Id, Collection<ArtifactCoordinate>> buildToArtifacts = buildsByComponent.computeIfAbsent(component, k -> new ConcurrentHashMap<>());
            Collection<ArtifactCoordinate> artifacts = buildToArtifacts.computeIfAbsent(buildId, k -> new ConcurrentLinkedQueue<>());
            artifacts.add(artifactCoordinate);
        }

        Collection<PncComponent> recordedComponents() {
            return buildsByComponent.keySet();
        }

        Collection<BuildRecord> recordedBuildsOf(PncComponent component) {
            return buildsByComponent.get(component).entrySet().stream()
                    .map(e->new BuildRecord(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }

    static class BuildRecord {
        private final PncBuild.Id buildId;
        private final List<ArtifactCoordinate> includedArtifacts;

        public BuildRecord(PncBuild.Id buildId, Collection<ArtifactCoordinate> includedArtifacts) {
            this.buildId = buildId;
            this.includedArtifacts = new ArrayList<>(includedArtifacts);
        }
    }

}
