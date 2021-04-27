package android.ripper.extension.robustness.strategy.perturb;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Perturb;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.transition.Event;
import it.unina.android.shared.ripper.model.transition.Input;

import java.util.List;
import java.util.Random;

public class RandomPerturb implements Perturb {

    private final static String callRecover = "recover(<ARGS0>);";
    private final static String WIDGETINDEX = "<widgetIndex>";
    private final static String WIDGETID = "<widgetId>";
    private final static String WIDGETNAME = "<widgetName>";
    private final static String WIDGETTYPE = "<widgetType>";
    private final static String EVENTTYPE = "<eventType>";
    private final static String INPUTTYPE = "<inputType>";
    private final static String VALUE = "<value>";
    private final static String fireEvent1 = "fireEvent(<widgetId>, <widgetIndex>, <widgetType>, <eventType>, <value>)";
    private final static String fireEvent2 = "fireEvent(<widgetId>, <widgetIndex>, <widgetName>, <widgetType>, <eventType>)";
    private final static String fireEvent3 = "fireEvent(<widgetIndex>,<widgetName>, <widgetType>, <eventType>)";
    private final static String fireEvent4 = "fireEvent(<widgetId>, <widgetIndex>, <widgetName>, <widgetType>, <eventType>, <value>)";
    private final static String fireEvent5 = "fireEvent(<widgetIndex>, <widgetName>, <widgetType>, <eventType>, <value>)";
    private final static String setInput = "setInput(<widgetId>, <inputType>, <value>)";

    /**
     * call perturb function in test suite generator.
     *
     * @return TestCase
     */
    @Override
    public String perturb(Transition transition, OperationFactory operationFactory) {

        StringBuilder testTrace = new StringBuilder();
        if (transition.getFromState().getIdle() > 0) {
            testTrace.append("idle(").append(transition.getFromState().getIdle()).append(");");
        }
        List<Event> events = transition.getEvents();
        Event event = null;
        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
            event = events.get(i);
            if (event.getInputs() != null) {
                for (Input input : event.getInputs()) {
                    testTrace.append(setInput
                            .replaceFirst(WIDGETID, String.valueOf(input.getWidget() == null ? "-1" : input.getWidget().getId()))
                            .replaceFirst(INPUTTYPE, "\"" + input.getInputType() + "\"")
                            .replaceFirst(VALUE, "\"" + input.getValue() + "\""))
                            .append(";");
                }
            } else {
                WidgetDescription wd = event.getWidget();
                if (wd != null) {
                    String eventType = event.getInteraction();
                    String widgetType = wd.getSimpleType();
                    int widgetId = wd.getId();
                    int widgetIndex = wd.getIndex();
                    String widgetName = wd.getName();
                    String value = wd.getValue();
                    if (widgetIndex == -1) {
                        if (value == null)
                            testTrace.append(fireEvent3.replaceFirst(WIDGETINDEX, String.valueOf(widgetIndex)).replaceFirst(WIDGETNAME, "\"" + widgetName + "\"").replaceFirst(WIDGETTYPE, "\"" + widgetType + "\"").replaceFirst(EVENTTYPE, "\"" + eventType + "\"")).append(";");
                        else
                            testTrace.append(fireEvent5.replaceFirst(WIDGETINDEX, String.valueOf(widgetIndex)).replaceFirst(WIDGETNAME, "\"" + widgetName + "\"").replaceFirst(WIDGETTYPE, "\"" + widgetType + "\"").replaceFirst(EVENTTYPE, "\"" + eventType + "\"").replaceFirst(VALUE, "\"" + value + "\"")).append(";");
                    } else {
                        if (widgetName == null && value != null)
                            testTrace.append(fireEvent1.replaceFirst(WIDGETID, String.valueOf(widgetId)).replaceFirst(WIDGETINDEX, String.valueOf(widgetIndex)).replaceFirst(WIDGETTYPE, widgetType).replaceFirst(EVENTTYPE, "\"" + eventType + "\"").replaceFirst(VALUE, "\"" + value + "\"")).append(";");
                        if (widgetName != null && value == null)
                            testTrace.append(fireEvent2.replaceFirst(WIDGETID, String.valueOf(widgetId)).replaceFirst(WIDGETINDEX, String.valueOf(widgetIndex)).replaceFirst(WIDGETNAME, "\"" + widgetName + "\"").replaceFirst(WIDGETTYPE, "\"" + widgetType + "\"").replaceFirst(EVENTTYPE, "\"" + eventType + "\"")).append(";");
                        if (widgetName != null && value != null)
                            testTrace.append(fireEvent4.replaceFirst(WIDGETID, String.valueOf(widgetId)).replaceFirst(WIDGETINDEX, String.valueOf(widgetIndex)).replaceFirst(WIDGETNAME, "\"" + widgetName + "\"").replaceFirst(WIDGETTYPE, "\"" + widgetType + "\"").replaceFirst(EVENTTYPE, "\"" + eventType + "\"").replaceFirst(VALUE, "\"" + value + "\"")).append(";");
                    }
                } else if (event.getInteraction() != null) {
                    testTrace.append("injectInteraction(null, \"").append(event.getInteraction()).append("\", ").append("null").append(");");
                }
            }
            if (i != eventsSize - 1 && event.getIdle() > 0) {
                testTrace.append("idle(").append(event.getIdle()).append(");");
            }
        }
        if (new Random().nextBoolean()) {
            testTrace.append(operationFactory.buildCall());
        }
        if (event != null && event.getIdle() > 0) {
            testTrace.append("idle(").append(event.getIdle()).append(");");
        }
        return testTrace.toString();
    }

    private String getDefaultUsingNull(String s) {
        if (s == null || s.length() == 0) return "null";
        else return "\"" + s + "\"";
    }

    private String getDefaultUsingEmpty(String s) {
        if (s == null || s.length() == 0) return "\"\"";
        else return "\"" + s + "\"";
    }

    @Override
    public String recover(OperationFactory operationFactory) {
//        return callRecover.replaceFirst("<ARGS0>", args[0]);
        return operationFactory.buildCall();
    }
}
