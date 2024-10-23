package org.jboss.set.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jboss.set.components.pnc.PncArtifact;
import org.jboss.set.components.pnc.PncBuild;
import org.jboss.set.components.pnc.PncComponent;
import org.jboss.set.components.pnc.PncManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wildfly.channel.ArtifactCoordinate;
import org.wildfly.channel.ChannelManifest;
import org.wildfly.channel.ChannelManifestMapper;
import org.wildfly.channel.Stream;

@ExtendWith(MockitoExtension.class)
class ManifestVerifierTest {

    @TempDir
    Path tempDir;

    @Mock
    private PncManager pncManager;

    @Test
    public void differentComponentVersionsFromTheSameBuild_Warning() throws Exception {
        final ManifestVerifier manifestVerifier = new ManifestVerifier(pncManager);
        final PncArtifact pncArtifactOne = new PncArtifact(
                new PncArtifact.Id("abcd"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-context", "jar", null, "1.29.0"),
                false);
        final PncArtifact pncArtifactTwo = new PncArtifact(
                new PncArtifact.Id("efgh"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-semconv", "jar", null, "1.29.0.alpha"),
                false);
        final PncBuild pncBuild = new PncBuild(new PncBuild.Id("build_1"), new PncComponent("opentelementry"));
        when(pncManager.getArtifact(any())).thenReturn(pncArtifactOne);
        when(pncManager.getBuildIdContainingArtifact(new PncArtifact.Id("abcd"))).thenReturn(pncBuild);
        when(pncManager.getArtifactsInBuild(new PncBuild.Id("build_1"))).thenReturn(List.of(pncArtifactOne, pncArtifactTwo));

        final ChannelManifest manifest = new ChannelManifest.Builder()
                .setSchemaVersion(ChannelManifestMapper.SCHEMA_VERSION_1_1_0)
                .addStreams(new Stream("io.opentelemetry", "opentelemetry-context", "1.29.0"))
                .addStreams(new Stream("io.opentelemetry", "opentelemetry-semconv", "1.29.0.alpha"))
                .build();
        final Path manifestFile = tempDir.resolve("test-manifest.yaml");
        Files.writeString(manifestFile, ChannelManifestMapper.toYaml(manifest));

        final VerificationResult verificationResult = manifestVerifier.verifyComponents(manifestFile.toUri().toURL());

        assertThat(verificationResult.getViolations()).isEmpty();
        assertThat(verificationResult.getWarnings())
                .map(Warning::getMessage)
                .containsOnly("[WARN] Different versions of artifact from the same build:");
    }

    @Test
    public void differentComponentVersionsFromTheDifferentBuild_Violation() throws Exception {
        final ManifestVerifier manifestVerifier = new ManifestVerifier(pncManager);
        final PncArtifact pncArtifactOneBuild1 = new PncArtifact(
                new PncArtifact.Id("abcd1"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-context", null, null, "1.29.0"),
                false);
        final PncArtifact pncArtifactTwoBuild1 = new PncArtifact(
                new PncArtifact.Id("efgh1"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-semconv", null, null, "1.29.0"),
                false);
        final PncArtifact pncArtifactOneBuild2 = new PncArtifact(
                new PncArtifact.Id("abcd2"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-context", null, null, "1.29.0.alpha"),
                false);
        final PncArtifact pncArtifactTwoBuild2 = new PncArtifact(
                new PncArtifact.Id("efgh2"),
                new ArtifactCoordinate("io.opentelemetry", "opentelemetry-semconv", null, null, "1.29.0.alpha"),
                false);
        final PncBuild pncBuildOne = new PncBuild(new PncBuild.Id("build_1"), new PncComponent("opentelemetry"));
        final PncBuild pncBuildTwo = new PncBuild(new PncBuild.Id("build_2"), new PncComponent("opentelemetry"));
        when(pncManager.getArtifact(pncArtifactOneBuild1.getCoordinate())).thenReturn(pncArtifactOneBuild1);
        when(pncManager.getArtifact(pncArtifactTwoBuild2.getCoordinate())).thenReturn(pncArtifactTwoBuild2);
        when(pncManager.getBuildIdContainingArtifact(new PncArtifact.Id("abcd1"))).thenReturn(pncBuildOne);
        when(pncManager.getBuildIdContainingArtifact(new PncArtifact.Id("efgh2"))).thenReturn(pncBuildTwo);
        when(pncManager.getArtifactsInBuild(new PncBuild.Id("build_1"))).thenReturn(List.of(pncArtifactOneBuild1, pncArtifactTwoBuild1));
        when(pncManager.getArtifactsInBuild(new PncBuild.Id("build_2"))).thenReturn(List.of(pncArtifactOneBuild2, pncArtifactTwoBuild2));

        final ChannelManifest manifest = new ChannelManifest.Builder()
                .setSchemaVersion(ChannelManifestMapper.SCHEMA_VERSION_1_1_0)
                .addStreams(new Stream("io.opentelemetry", "opentelemetry-context", "1.29.0"))
                .addStreams(new Stream("io.opentelemetry", "opentelemetry-semconv", "1.29.0.alpha"))
                .build();
        final Path manifestFile = tempDir.resolve("test-manifest.yaml");
        Files.writeString(manifestFile, ChannelManifestMapper.toYaml(manifest));

        final VerificationResult verificationResult = manifestVerifier.verifyComponents(manifestFile.toUri().toURL());

        assertThat(verificationResult.getWarnings()).isEmpty();
        assertThat(verificationResult.getViolations())
                .containsOnly(new Violation("opentelemetry", Map.of(
                        "1.29.0", List.of(pncArtifactOneBuild1.getCoordinate()),
                        "1.29.0.alpha", List.of(pncArtifactTwoBuild2.getCoordinate())
                )));
    }

}