package android.ripper.extension.robustness.output;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;
import android.ripper.extension.robustness.strategy.Perturb;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class TestSuiteGenerator {


    private final String AUT_PACKAGE;
    private final Coverage coverage;
    private final Perturb perturb;
    private final String testSuitePath = "TestSuite.java";

    public void generate(Set<Transition> transitions) {
        //TODO
        //for each transition, generate testcase
        StringBuilder testTrace = new StringBuilder();
        int id = 0;

        for (Transition transition : coverage.cherryPick(transitions)) {
            //always start running from the root state
            //fire task's events, insert perturbation according to (TODO) strategy
            //check final state: tell the differences/similarity only
            //if unable to reach final state, i.e. stuck at some state
            //report, manually check for true/false positive later
            testTrace.append("    //Generated from trace " + id + "\n");
            testTrace.append("    public void testTrace" + id + " () {");
            testTrace.append(perturb.recover(Boolean.toString(true)));
            testTrace.append(perturb.perturb(transition, Boolean.toString(false)));
        }
        //TODO check whether reach final state
    }

    public TestSuiteGenerator(String AUT_PACKAGE, String coverage, String perturb, String CLASS_NAME) {
        this.AUT_PACKAGE = AUT_PACKAGE;
        this.coverage = Coverage.of(coverage);
        this.perturb = Perturb.of(perturb);
        String target = "";
        try {
            Path path = Paths.get("/Users/pkun/IdeaProjects/android-robustness/AndroidRipperDriver/src/android/ripper/extension/robustness/output/TestSuiteExample.txt");
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            target = target.replaceAll("<PACKAGE_NAME>", AUT_PACKAGE);
            target = target.replaceAll("<CLASS_NAME>", CLASS_NAME);
            Files.write(Paths.get(testSuitePath), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


    public static void main(String[] args) {
        System.out.println(Boolean.toString(true));
    }

}
