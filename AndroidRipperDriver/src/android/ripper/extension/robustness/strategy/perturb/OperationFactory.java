package android.ripper.extension.robustness.strategy.perturb;

import android.ripper.extension.robustness.exception.RipperIllegalArgException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: pkun
 * @CreateTime: 2021-04-04 20:15
 */
public class OperationFactory {

    /**
     * this class could auto config a perturb method like spring security HttpSecurity.
     * the best practice is
     * 1. design the perturb you want to use before write any code.
     * 2. decide the order and content of the params, now only support the basic data type like int,double,boolean and so on.
     * 3. write the content of the perturb function like build logo.
     * 4. call buildMethod to replace <perturb> in TestSuiteExample and call buildCall to add caller in testcase.
     */
    private final StringBuilder factory;
    private final String soloReference;
    private final String getActivityMonitor = "getActivityMonitor()";
    private final String getConfig = "getConfig()";
    private final String getViews = "getViews()";
    private final String setMobileData = "setMobileData(<ARG0>)";
    private final String setWiFiData = "setWiFiData(<ARG0>)";
    private final String index = "x";
    private final String placeholder = "<ARG" + index + ">";
    private final String param = "obj_";
    private final String THIS = "this.";
    private int methodId = 0; // mark the id of perturb method;
    private List<Object> params;
    private static int factoryId = 0;
    private final int argc;

    public OperationFactory(String soloReference, int argc) {
        factory = new StringBuilder();
        this.soloReference = soloReference;
        this.argc = argc;
//        factory.append("protected void perturb(<PARAM>){");
        params = new ArrayList<>();
        methodId = factoryId;
        factoryId++;
    }

    public OperationFactory addGetActivityMonitor(List<Integer> paramIndex) {
        factory.append(THIS).append(soloReference).append(".").append(replacePlaceholder(setWiFiData, paramIndex)).append(";");
        return this;
    }

    public OperationFactory setMobileData(int[] paramIndex) {
        List<Integer> paramIndex_ = new ArrayList<>();
        for (int i : paramIndex) {
            paramIndex_.add(i);
        }
        return setMobileData(paramIndex_);
    }

    public OperationFactory setMobileData(List<Integer> paramIndex) {
        factory.append(THIS).append(soloReference).append(".").append(replacePlaceholder(setMobileData, paramIndex)).append(";");
        return this;
    }

    /**
     * this method will return a string which call this perturb.
     *
     * @return
     */
    public String buildCall() {
        StringBuilder callBuilder = new StringBuilder();
        callBuilder.append("operation").append(methodId).append("(");
        for(int i = 0;i<params.size();i++){
            callBuilder.append(params.get(i).toString());
            if(i!=params.size()-1) callBuilder.append(",");
        }
        callBuilder.append(");");
        return callBuilder.toString();
    }

    /**
     * this method will return a function which developer build before.
     *
     * @return
     */
    public String buildMethod() {
        factory.reverse().append(new StringBuilder().append("protect void operation").append(methodId).append("(").append(buildParam()).append("){").reverse()).reverse();
        factory.append("}");
        return factory.toString();
    }

    public String buildParam() {
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            Object o = params.get(i);
            if (o instanceof Integer) {
                paramsBuilder.append("Integer ").append(param).append(i);
            } else if (o instanceof Long) {
                paramsBuilder.append("Long ").append(param).append(i);
            } else if (o instanceof String) {
                paramsBuilder.append("String ").append(param).append(i);
            } else if (o instanceof Boolean) {
                paramsBuilder.append("Boolean ").append(param).append(i);
            } else if (o instanceof Character) {
                paramsBuilder.append("Character ").append(param).append(i);
            } else if (o instanceof Byte) {
                paramsBuilder.append("Byte ").append(param).append(i);
            } else if (o instanceof Short) {
                paramsBuilder.append("Short ").append(param).append(i);
            } else if (o instanceof Float) {
                paramsBuilder.append("Float ").append(param).append(i);
            } else if (o instanceof Double) {
                paramsBuilder.append("Double ").append(param).append(i);
            } else {
                throw new RipperIllegalArgException(OperationFactory.class, "buildParam", o.getClass() + o.toString(), "Types other than basic data types are not currently supported");
            }
            if (i != params.size() - 1) paramsBuilder.append(",");
        }
        return paramsBuilder.toString();
    }

    public String replacePlaceholder(String method, List<Integer> paramIndex) {
        String temp = method;
        for (int i = 0; i < paramIndex.size(); i++) {
            String var1 = placeholder.replaceFirst(index, String.valueOf(i));
            temp = temp.replaceFirst(var1, param + i);
        }
        return temp;
    }

    public OperationFactory addParam(Object o) {
        if(params.size()==argc) throw new RipperIllegalArgException(OperationFactory.class, "addParam", o.getClass() + o.toString(), "This factory can only enter " + argc + " parameters");
        params.add(o);
        return this;
    }

    public OperationFactory modifyParam(Object o, int index){
        params.set(index, o);
        return this;
    }

    public static void main(String[] args) {
        OperationFactory perturbFactory = new OperationFactory("solo", 1);
        perturbFactory.addParam(false);
        perturbFactory.setMobileData(new int[]{0});
        OperationFactory recoverFactory = new OperationFactory("solo", 1);
        recoverFactory.addParam(true);
        recoverFactory.setMobileData(new int[]{0});
        System.out.println(perturbFactory.buildCall());
        System.out.println(perturbFactory.buildMethod());
        System.out.println(recoverFactory.buildMethod());
    }

}
