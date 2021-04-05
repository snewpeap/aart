package android.ripper.extension.robustness.strategy.perturb;

import android.ripper.extension.robustness.driver.AARTDriver;
import android.ripper.extension.robustness.exception.RipperIllegalArgException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: pkun
 * @CreateTime: 2021-04-04 20:15
 */
public class PerturbFactory {

    /**
     * this class could auto config a perturb method like spring security HttpSecurity.
     * the best practice is
     * 1. design the perturb you want to use before write any code.
     * 2. decide the order and content of the params, now only support the basic data type like int,double,boolean and so on.
     * 3. write the content of the perturb function like build logo.
     * 4. call buildMethod to replace <perturb> in TestSuiteExample and call buildCall to add caller in testcase.
     */
    public final StringBuilder factory;
    public final String soloReference;
    public final String getActivityMonitor = "getActivityMonitor()";
    public final String getConfig = "getConfig()";
    public final String getViews = "getViews()";
    public final String setMobileData = "setMobileDate(<ARG0>)";
    public final String setWiFiData = "setWiFiData(<ARG0>)";
    public final String index = "x";
    public final String placeholder = "<ARG"+index+">";
    public final String param = "obj_";
    public final String THIS="this.";
    public int methodId=0; // mark the id of perturb method;
    public List<Object> params;
    public final int argc;

    public PerturbFactory(String soloReference, int argc){
        factory = new StringBuilder();
        this.soloReference = soloReference;
        this.argc = argc;
//        factory.append("protected void perturb(<PARAM>){");
        params = new ArrayList<>();
    }

    public PerturbFactory addGetActivityMonitor(List<Integer> paramIndex){
        factory.append(THIS).append(soloReference).append(".").append(replacePlaceholder(setWiFiData, paramIndex));
        return this;
    }

    public PerturbFactory setMobileData(List<Integer> paramIndex){
        factory.append(THIS).append(soloReference).append(".").append(replacePlaceholder(setMobileData, paramIndex));
        return this;
    }

    /**
     * this method will return a string which call this perturb.
     * @return
     */
    public String buildCall(){
        return null;
    }

    /**
     * this method will return a function which developer build before.
     * @return
     */
    public String buildMethod(){
        factory.reverse().append(new StringBuilder().append("protect void perturb").append(methodId).append("(").append(buildParam()).append("){").reverse()).reverse();
        factory.append("}");
        return factory.toString();
    }

    public String buildParam(){
        StringBuilder paramsBuilder = new StringBuilder();
        for(int i = 0;i<params.size();i++){
            Object o = params.get(i);
            if(o instanceof Integer){
                paramsBuilder.append("Integer ").append(param).append(i);
            }
            else if(o instanceof Long){
                paramsBuilder.append("Long ").append(param).append(i);
            }
            else if(o instanceof String){
                paramsBuilder.append("String ").append(param).append(i);
            }
            else if(o instanceof Boolean){
                paramsBuilder.append("Boolean ").append(param).append(i);
            }
            else if(o instanceof Character){
                paramsBuilder.append("Character ").append(param).append(i);
            }
            else if(o instanceof Byte){
                paramsBuilder.append("Byte ").append(param).append(i);
            }
            else if(o instanceof Short){
                paramsBuilder.append("Short ").append(param).append(i);
            }
            else if(o instanceof Float){
                paramsBuilder.append("Float ").append(param).append(i);
            }
            else if(o instanceof Double){
                paramsBuilder.append("Double ").append(param).append(i);
            }
            else{
                throw new RipperIllegalArgException(PerturbFactory.class, "buildParam", o.getClass() + o.toString(), "Types other than basic data types are not currently supported");
            }
        }
        return paramsBuilder.toString();
    }

    public String replacePlaceholder(String method, List<Integer> paramIndex){
        String temp = method;
        for(int i = 0;i<paramIndex.size();i++){
            String var1 = placeholder.replaceFirst(index, String.valueOf(i));
            temp = temp.replaceFirst(var1, param+i);
        }
        return temp;
    }

    public PerturbFactory addParam(Object o){
        params.add(o);
        return this;
    }

}
