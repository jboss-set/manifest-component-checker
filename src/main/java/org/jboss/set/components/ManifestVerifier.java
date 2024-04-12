package org.jboss.set.components;

import org.apache.commons.lang3.StringUtils;
import org.jboss.set.components.pnc.PncArtifact;
import org.jboss.set.components.pnc.PncBuild;
import org.jboss.set.components.pnc.PncManagerImpl;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.Stream;

import java.net.MalformedURLException;
import java.net.URI;
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
    final PncManagerImpl pncManager;

    public ManifestVerifier(PncManagerImpl pncManager) {
        this.pncManager = pncManager;
    }

    public VerificationResult verifyComponents(URL manifestURL) throws MalformedURLException {
        final ChannelManifest manifest = ChannelManifestMapper.from(manifestURL);
        final Collection<Stream> streams = manifest.getStreams();

        // GA:V
        final ConcurrentMap<String, Collection<ArtifactCoordinate>> components = new ConcurrentHashMap<>();
        final Collection<ArtifactCoordinate> imported = new ConcurrentLinkedQueue<>();
        final Collection<ArtifactCoordinate> ungrouped = new ArrayList<>();

        // cache GA:Component
        final Map<String, String> cache = new ConcurrentHashMap<>();

        AtomicInteger counter = new AtomicInteger(0);
        streams.parallelStream().forEach((stream)-> {
            final String cacheKey = toKey(stream.getGroupId(), stream.getArtifactId(), stream.getVersion());
            final ArtifactCoordinate artifactCoordinate = stream2Coord(stream);
            System.out.printf("Resolving [%d/%d]: %s%n", counter.getAndIncrement(), streams.size(), artifactCoordinate);
            if (cache.containsKey(cacheKey)) {
                components.get(cache.get(cacheKey)).add(artifactCoordinate);
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

                final List<PncArtifact> componentArtifacts = pncManager.getArtifactsInBuild(build.getId());

                for (PncArtifact componentArtifact : componentArtifacts) {
                    final String compKey = toKey(componentArtifact.getCoordinate().getGroupId(),
                            componentArtifact.getCoordinate().getArtifactId(), componentArtifact.getCoordinate().getVersion());
                    cache.putIfAbsent(compKey, build.getBrewComponent());
                }
            }
        });

//        boolean violationsFound = false;

        final VerificationResult res = new VerificationResult();

        for (String componentName : components.keySet()) {
            final Collection<ArtifactCoordinate> artifactCoordinates = components.get(componentName);
            final Map<String, List<ArtifactCoordinate>> artifactsByVersion = artifactCoordinates.stream()
                    .collect(Collectors.groupingBy(ArtifactCoordinate::getVersion));
            if (artifactsByVersion.size() > 1) {
//                violationsFound = true;
                res.addViolation(new Violation(componentName, artifactsByVersion));
//                System.out.println("[ERROR] Component " + componentName + " has multiple versions");
//                for (String version : artifactsByVersion.keySet()) {
//                    System.out.println("        " + version);
//                    for (ArtifactCoordinate artifactCoordinate : artifactsByVersion.get(version)) {
//                        System.out.println("          * " + artifactCoordinate.getGroupId() + ":" + artifactCoordinate.getArtifactId());
//                    }
//
//                }
            }
        }


        if (!imported.isEmpty()) {
//            System.out.println("[INFO] Ignored imported artifacts:");
            res.addWarning(new Warning("[INFO] Ignored imported artifacts:", new ArrayList<>(imported)));
        }
//        imported.forEach(ac->new Warning(""));
//        imported.forEach(ac->System.out.println("          * " + ac.getGroupId() + ":" + ac.getArtifactId()));

        if (!ungrouped.isEmpty()) {
//            System.out.println("[WARN] Unable to determine components of:");
            res.addWarning(new Warning("[WARN] Unable to determine components of:", new ArrayList<>(imported)));
        }
//        ungrouped.forEach(ac->System.out.println("          * " + ac.getGroupId() + ":" + ac.getArtifactId()));

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
}
