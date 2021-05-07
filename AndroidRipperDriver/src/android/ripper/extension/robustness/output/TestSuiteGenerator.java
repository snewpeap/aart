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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TestSuiteGenerator {

    private final String AUT_PACKAGE;
    private final Coverage coverage;
    private final Perturb perturb;
    private final String testSuitePath = "TestSuite.java";
    private final String testcase = "<TEST_TRACE_ID>";
    private final String perturb_ = "<PERTURB_FUNCTION>";
    private final String reportPath = "reportPath.txt";
    private static int CLASS_INDEX = -1;
    private static String CLASS_NAME = "StateContainer";
    private static String STATE_NAME = "State";
    private static int STATE_START = 0;
    private static int STATE_END = 0;
    private static int STATE_NUM = 0;
    private static int MAX_STRING_LENGTH = 8096;
    private static int MAX_CAPACITY = 10;
    private static String CONCAT_FUNC = "concat";

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
        createContainer();
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
            testTrace.append("try {");
            testTrace.append(perturb.recover(recoverFactory));
            testTrace.append(perturb.perturb(transition, perturbFactory));
            testTrace.append("} catch (Exception ignored) {} ");
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
            testTrace.append("report(").append("StringEscapeUtils.unescapeJava(").append(concatStateFromContainer()).append(")").append(", actual, ").append(id).append(");\n");

            STATE_START = STATE_END + 1;
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
        if (STATE_NUM >= MAX_CAPACITY) {
            createContainer();
        }
        addState(state);
    }

    private void addState(String state) {
        StringBuilder stringBuilder = new StringBuilder();
        int stringLen = state.length();
        int i = 0;
        int j = i + MAX_STRING_LENGTH;
        int index = 0;
        while (i < stringLen) {
            if (j < stringLen && state.charAt(j - 1) == '\\') j = j + 1;
            if (j < stringLen && state.charAt(j-1) == 'u' && state.charAt(j-2) == '\\') {
                j = j + 4;
            }
            if (j > stringLen) j = stringLen;
            stringBuilder.append("public final static String ").append(STATE_NAME).append(STATE_START + index++).append(" = \"").append(state, i, j).append("\";");
            i = j;
            j = i + MAX_STRING_LENGTH;
        }
        STATE_END = STATE_START + index - 1;
        STATE_NUM = STATE_NUM + index - 1;
        if (STATE_NUM < MAX_CAPACITY) stringBuilder.append("<STATE_INDEX>");
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

    private String concatStateFromContainer() {
        //StateContainer0.State0.concat(StateContainer0.State1.concat())
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CLASS_NAME).append(CLASS_INDEX).append(".").append(STATE_NAME).append(STATE_START);
        if (STATE_START + 1 <= STATE_END) stringBuilder.append(".").append(CONCAT_FUNC).append("(");
        for (int i = STATE_START + 1; i <= STATE_END; i++) {
            stringBuilder.append(CLASS_NAME).append(CLASS_INDEX).append(".").append(STATE_NAME).append(i);
            if (i != STATE_END)
                stringBuilder.append(".").append(CONCAT_FUNC).append("(");
        }
        for (int i = STATE_START + 1; i <= STATE_END; i++) {
            stringBuilder.append(")");
        }
        return stringBuilder.toString();
    }

    private void createContainer() {
        String target;
        try {
            URI templatePath = Objects.requireNonNull(
                    getClass().getClassLoader().getResource("StateContainerExample.java")).toURI();
            Path path = Paths.get(templatePath);
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            CLASS_INDEX++;
            STATE_NUM = 0;
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

    public static void reMark(Set<Transition> transitions) {
        Set<String> fromStateUid = new HashSet<>();
        transitions.forEach(t -> {
            fromStateUid.add(t.getFromState().getUid());
        });
        transitions.forEach(t -> {
            t.getToState().setReentered(fromStateUid.contains(t.getToState().getUid()));
        });
    }

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        TestSuiteGenerator testSuiteGenerator = new TestSuiteGenerator("", "lifts", "all", args[0]);
        System.out.println(System.getenv());
        Set<Transition> transitions = null;
        try {
            File file = new File(args[1]);
            transitions = objectMapper.readValue(file, new TypeReference<Set<Transition>>() {
            });
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        assert transitions != null;
        reMark(transitions);
        testSuiteGenerator.generate(transitions);
    }

}
