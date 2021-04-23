package android.ripper.extension.robustness.output;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Coverage;
import android.ripper.extension.robustness.strategy.Perturb;
import android.ripper.extension.robustness.strategy.perturb.OperationFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void generate(Set<Transition> transitions) {
        //TODO
        //for each transition, generate testcase
        StringBuilder testTrace = new StringBuilder();
        int id = 0;
        OperationFactory perturbFactory = new OperationFactory("solo", 1);
        perturbFactory.addParam(false);
        perturbFactory.setMobileData(new int[]{0});
        OperationFactory recoverFactory = new OperationFactory("solo", 1);
        recoverFactory.addParam(true);
        recoverFactory.setMobileData(new int[]{0});


        for (Transition transition : coverage.cherryPick(transitions)) {
            //always start running from the root state
            //fire task's events, insert perturbation according to (TODO) strategy
            //check final state: tell the differences/similarity only
            //if unable to reach final state, i.e. stuck at some state
            //report, manually check for true/false positive later
            testTrace.append("    //Generated from trace ").append(id).append("\n");
            testTrace.append("    public void testTrace").append(id).append(" () {\n");
            testTrace.append(perturb.recover(recoverFactory));
            testTrace.append(perturb.perturb(transition, perturbFactory));
            ObjectMapper objectMapper = new ObjectMapper();
            String shouldBeState="";
            try
            {
                shouldBeState = objectMapper.writeValueAsString(transition.getToState());
            }catch (JsonProcessingException jsonProcessingException)
            {
                jsonProcessingException.printStackTrace();
            }
            testTrace.append("String shouldBeStateSer = \"").append(shouldBeState).append("\";");
            testTrace.append("State shouldBeState = objectMapper.readValue(shouldBeStateSer, State.class);");
//            shouldBeState.put(id, transition.getToState());

            testTrace.append("report(").append("shouldBeState").append(", new State(extractor.extract()));\n");
            //TODO check the rate of coverage
            testTrace.append("}\n");
            id++;
        }
        //TODO add Serializable here
        ReplaceTestFile(testcase, testTrace.toString());
        ReplaceTestFile(perturb_, perturbFactory.buildMethod() + recoverFactory.buildMethod());
//        ADSerializable();
    }

    private void ReplaceTestFile(String pattern, String target){
        Path path = Paths.get(testSuitePath);
        try {
            String s = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            s = s.replaceFirst(pattern, target);
            Files.write(Paths.get(testSuitePath), s.getBytes(StandardCharsets.UTF_8));
        }catch (IOException io){
            io.printStackTrace();
        }
    }

//    public void ADSerializable(){
//        Path path = Paths.get("SerializableState.txt");
//        try{
//            BufferedWriter bufferedWriter = new BufferedWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE));
//            bufferedWriter.write(JSONObject.toJSONString(shouldBeState));
//            bufferedWriter.close();
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//    }

//    public HashMap ADDeserialized(){
//
//        Path path = Paths.get("SerializableState.txt");
//        try{
//            BufferedReader bufferedReader = new BufferedReader(Files.newBufferedReader(path, StandardCharsets.UTF_8));
//            String s = bufferedReader.readLine();
//            JSONObject jsonObject = JSON.parseObject(s);
//            HashMap<Integer, ActivityDescription> map = new HashMap<>();
//            for(String key : jsonObject.keySet()){
//                ActivityDescription ad = jsonObject.getObject(key, ActivityDescription.class);
//                map.put(Integer.parseInt(key), ad);
//            }
//            return map;
//        }catch (IOException e){
//            e.printStackTrace();
//            return null;
//        }
//    }


    public TestSuiteGenerator(String AUT_PACKAGE, String coverage, String perturb, String CLASS_NAME) {
        this.AUT_PACKAGE = AUT_PACKAGE;
        this.coverage = Coverage.of(coverage);
        this.perturb = Perturb.of(perturb);
        String target = "";
        System.out.println("Starting TestSuiteGenerator...");
        try {
            URI templatePath = Objects.requireNonNull(
                    getClass().getClassLoader().getResource("TestSuiteExample.txt")).toURI();
            Path path = Paths.get(templatePath);
            target = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            target = target.replaceAll("<CLASS_NAME>", "\"" + CLASS_NAME + "\"");
            Files.write(Paths.get(testSuitePath), target.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | URISyntaxException ioException) {
            ioException.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException{
        StringBuilder stringBuilder = new StringBuilder();
        String s = "aklsjdkl\nlaksdjksa\n\t;kasdka\tlaskjdkals\n";
        System.out.println("asdjksadjkasldjasld\\nasdasdad");
        s = s.replaceAll("\\n", "\\\\n").replaceAll("\\t", "\\\\t");
        stringBuilder.append(s);
        System.out.println(stringBuilder);
    }
}
