package org.jboss.set.components;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ManifestVerifier {
    final PncManagerImpl pncManager;

    public ManifestVerifier(PncManagerImpl pncManager) {
        this.pncManager = pncManager;
    }

    public void verifyComponents(URL manifestURL) throws MalformedURLException {
        final ChannelManifest manifest = ChannelManifestMapper.from(manifestURL);
        final Collection<Stream> streams = manifest.getStreams();

        // GA:V
        final Map<String, Set<ArtifactCoordinate>> components = new HashMap<>();
        final List<ArtifactCoordinate> imported = new ArrayList<>();
        final List<ArtifactCoordinate> ungrouped = new ArrayList<>();

        // cache GA:Component
        final Map<String, String> cache = new HashMap<>();

        int counter = 0;
        for (Stream stream : streams) {
            final String cacheKey = stream.getGroupId() + ":" + stream.getArtifactId() + ":" + stream.getVersion();
            final ArtifactCoordinate artifactCoordinate = stream2Coord(stream);
            System.out.printf("Resolving [%d/%d]: %s%n", counter++, streams.size(), artifactCoordinate);
            if (cache.containsKey(cacheKey)) {
                components.get(cache.get(cacheKey)).add(artifactCoordinate);
            } else {

                final PncArtifact artifact = pncManager.getArtifact(artifactCoordinate);

                if (artifact.isImported()) {
                    imported.add(artifact.getCoordinate());
                    continue;
                }

                final PncBuild build = pncManager.getBuildIdContainingArtifact(artifact.getId());

                if (build == null) {
                    ungrouped.add(artifact.getCoordinate());
                    continue;
                }

                if (!components.containsKey(build.getBrewComponent())) {
                    components.put(build.getBrewComponent(), new HashSet<>());
                }

                components.get(build.getBrewComponent()).add(artifact.getCoordinate());

                final List<PncArtifact> componentArtifacts = pncManager.getArtifactsInBuild(build.getId());

                for (PncArtifact componentArtifact : componentArtifacts) {
                    final String compKey = componentArtifact.getCoordinate().getGroupId() + ":" + componentArtifact.getCoordinate().getArtifactId();
                    cache.put(compKey, build.getBrewComponent());
                }
            }
        }

        for (String key : components.keySet()) {
            final Set<ArtifactCoordinate> artifactCoordinates = components.get(key);
            final Map<String, List<ArtifactCoordinate>> artifactsByVersion = artifactCoordinates.stream()
                    .collect(Collectors.groupingBy(ArtifactCoordinate::getVersion));
            if (artifactsByVersion.size() > 1) {
                System.out.println("[ERROR] Component " + key + " has multiple versions");
                for (String version : artifactsByVersion.keySet()) {
                    System.out.println("        " + version);
                    for (ArtifactCoordinate artifactCoordinate : artifactsByVersion.get(version)) {
                        System.out.println("          * " + artifactCoordinate.getGroupId() + ":" + artifactCoordinate.getArtifactId());
                    }

                }
            }
        }


        if (!imported.isEmpty()) {
            System.out.println("[INFO] Ignored imported artifacts:");
        }
        imported.forEach(ac->System.out.println("          * " + ac.getGroupId() + ":" + ac.getArtifactId()));

        if (!ungrouped.isEmpty()) {
            System.out.println("[WARN] Unable to determine components of:");
        }
        ungrouped.forEach(ac->System.out.println("          * " + ac.getGroupId() + ":" + ac.getArtifactId()));
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
}
