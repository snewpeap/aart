package android.ripper.extension.robustness.output;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;
import android.ripper.extension.robustness.strategy.Perturb;
import android.ripper.extension.robustness.strategy.perturb.OperationFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.Set;

public class TestSuiteGenerator {

    private final String AUT_PACKAGE;
    private final Coverage coverage;
    private final Perturb perturb;
    private final String testSuitePath = "TestSuite.java";
    private final String testcase = "<TEST_TRACE_ID>";
    private final String perturb_ = "<PERTURB_FUNCTION>";
    private final String reportPath = "reportPath.txt";
    private static int CLASS_INDEX = -1;
    private static int STATE_INDEX = 0;
    private static int MAX_CAPACITY = 15;

    public void generate(Set<Transition> transitions) {
        //for each transition, generate testcase
        StringBuilder testTrace = new StringBuilder();
        int id = 0;
        OperationFactory perturbFactory = new OperationFactory("solo", 1);
        perturbFactory.addParam(false);
        perturbFactory.setMobileData(new int[]{0});
        OperationFactory recoverFactory = new OperationFactory("solo", 1);
        recoverFactory.addParam(true);
        recoverFactory.setMobileData(new int[]{0});

        try {
            Path path = Paths.get("transitions.json");
            Files.write(path, new ObjectMapper().writeValueAsBytes(transitions));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        for (Transition transition : coverage.cherryPick(transitions)) {
            //always start running from the root state
            //fire task's events, insert perturbation according to strategy
            //check final state: tell the differences/similarity only
            //if unable to reach final state, i.e. stuck at some state
            //report, manually check for true/false positive later
            testTrace.append("    //Generated from trace ").append(id).append("\n");
            testTrace.append("    public void testTrace").append(id).append(" ()   throws JsonProcessingException {\n");
            testTrace.append(perturb.recover(recoverFactory));
            testTrace.append(perturb.perturb(transition, perturbFactory));
            ObjectMapper objectMapper = new ObjectMapper();
            String shouldBeState = "";
            try {
                // get shouldBeState ( state )
                shouldBeState = objectMapper.writeValueAsString(transition.getToState());
            } catch (JsonProcessingException jsonProcessingException) {
                jsonProcessingException.printStackTrace();
            }
            // add shouldBeState into file
            addIntoStateContainerOrCreate(StringEscapeUtils.escapeJava(shouldBeState));
            testTrace.append("String actual = objectMapper.writeValueAsString(new State(extractor.extract()));");
//            shouldBeState.put(id, transition.getToState());
            testTrace.append("report(").append("StringEscapeUtils.unescapeJava(").append(getStateFromContainer()).append(")").append(", actual, ").append(id).append(");\n");

            STATE_INDEX++;
            testTrace.append("}\n");
            id++;
        }
        //TODO add Serializable here
        replaceTestFile(testcase, testTrace.toString());
//        replaceTestFile(perturb_, perturbFactory.buildMethod() + recoverFactory.buildMethod());
        String target;
        try {
            Path path = Paths.get("StateContainer" + CLASS_INDEX + ".java");
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            target = target.replaceAll("<STATE_INDEX>", "");
            Files.write(Paths.get("StateContainer" + CLASS_INDEX + ".java"), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
//        ADSerializable();
    }

    private void replaceTestFile(String pattern, String target) {
        Path path = Paths.get(testSuitePath);
        try {
            String s = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            s = s.replaceFirst(pattern, target);
            Files.write(Paths.get(testSuitePath), s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private void addIntoStateContainerOrCreate(String state) {
        if (STATE_INDEX % MAX_CAPACITY == 0) {
            createContainer();
        }
        addState(state);
    }

    private void addState(String state) {
        StringBuilder stringBuilder  = new StringBuilder();
        stringBuilder.append("public final static String ").append("State").append(STATE_INDEX).append(" = \"").append(state).append("\";");
        if(STATE_INDEX % MAX_CAPACITY != MAX_CAPACITY - 1) stringBuilder.append("<STATE_INDEX>");
        String target;
        try {
            Path path = Paths.get("StateContainer" + CLASS_INDEX + ".java");
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            target = target.replaceAll("<STATE_INDEX>", StringEscapeUtils.escapeJava(stringBuilder.toString()));
            Files.write(Paths.get("StateContainer" + CLASS_INDEX + ".java"), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private String getStateFromContainer()
    {
        return "StateContainer" + CLASS_INDEX + "." + "State" + STATE_INDEX;
    }

    private void createContainer() {
        String target;
        try {
            URI templatePath = Objects.requireNonNull(
                    getClass().getClassLoader().getResource("StateContainerExample.java")).toURI();
            Path path = Paths.get(templatePath);
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            CLASS_INDEX++;
            target = target.replaceAll("<CLASS_INDEX>", String.valueOf(CLASS_INDEX));
            Files.write(Paths.get("StateContainer" + CLASS_INDEX + ".java"), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | URISyntaxException ioException) {
            ioException.printStackTrace();
        }
    }


    public TestSuiteGenerator(String AUT_PACKAGE, String coverage, String perturb, String CLASS_NAME) {
        this.AUT_PACKAGE = AUT_PACKAGE;
        this.coverage = Coverage.of(coverage);
        this.perturb = Perturb.of(perturb);
        String target;
        System.out.println("Starting TestSuiteGenerator...");
        try {
            URI templatePath = Objects.requireNonNull(
                    getClass().getClassLoader().getResource("TestSuiteExample.java")).toURI();
            Path path = Paths.get(templatePath);
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            target = target.replaceAll("<CLASS_NAME>", "\"" + CLASS_NAME + "\"");
            Files.write(Paths.get(testSuitePath), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | URISyntaxException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TestSuiteGenerator testSuiteGenerator = new TestSuiteGenerator("", "lifts", "all", args[0]);
        System.out.println(System.getenv());
        Set<Transition> transitions = null;
        try {
            File file = new File(args[1]);
            transitions = objectMapper.readValue(file, new TypeReference<Set<Transition>>() {});
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        testSuiteGenerator.generate(transitions);
    }

}
