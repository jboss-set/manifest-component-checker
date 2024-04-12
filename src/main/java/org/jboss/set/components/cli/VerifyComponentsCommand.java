package org.jboss.set.components.cli;

import org.jboss.set.components.ManifestVerifier;
import org.jboss.set.components.VerificationResult;
import org.jboss.set.components.pnc.PncManagerImpl;
import picocli.CommandLine;

import java.net.URL;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "verify-components", description = "Checks if the manifest contains and components with more then one of version of artifact.\n" +
        "The components are based on the PNC artifact builds.")
public class VerifyComponentsCommand implements Callable<Integer> {

    @CommandLine.Option(names = {CliConstants.H, CliConstants.HELP}, usageHelp = true)
    boolean help;

    @CommandLine.Option(names = {"--manifest-url"}, required = true, description = "An URL of the fixed-version manifest to check")
    URL manifestUrl;

    @CommandLine.Option(names = {"--pnc-url"}, required = true, description = "An URL of the PNC API gateway used to build artifacts in the manifest")
    URL pncUrl;


    @Override
    public Integer call() throws Exception {
        final ManifestVerifier manifestVerifier = new ManifestVerifier(new PncManagerImpl(pncUrl));

        final VerificationResult verificationResult = manifestVerifier.verifyComponents(manifestUrl);

        verificationResult.getViolations().forEach(v->System.out.println(v.print()));
        verificationResult.getWarnings().forEach(w->System.out.println(w.print()));

        return verificationResult.getViolations().isEmpty()? CliConstants.ReturnCodes.SUCCESS: CliConstants.ReturnCodes.FAILURE;
    }

    public static void main(String[] args) throws Exception {
        CommandLine commandLine = createCommandLine();
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static CommandLine createCommandLine() {
        CommandLine commandLine = new CommandLine(new VerifyComponentsCommand());

        commandLine.setUsageHelpAutoWidth(true);
        return commandLine;
    }
}
