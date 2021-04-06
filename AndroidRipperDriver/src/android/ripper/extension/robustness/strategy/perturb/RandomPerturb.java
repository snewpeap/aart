package android.ripper.extension.robustness.strategy.perturb;

import android.ripper.extension.robustness.model.Transition;
import android.ripper.extension.robustness.strategy.Perturb;
import it.unina.android.shared.ripper.model.state.WidgetDescription;
import it.unina.android.shared.ripper.model.transition.Event;

import java.util.List;
import java.util.Random;

public class RandomPerturb implements Perturb {

    private final static String callRecover = "recover(<ARGS0>);";
    private final static String fireEvent = "fireEvent (<widgetId>, <widgetIndex>, <widgetName>, <widgetType>, <eventType>, <value>)";

    /**
     * call perturb function in test suite generator.
     * @return
     */
    @Override
    public String perturb(Transition transition,  OperationFactory operationFactory){

        StringBuilder testTrace = new StringBuilder();
        List<Event> events = transition.getEvents();
        for (Event event : events) {
            WidgetDescription wd = event.getWidget();
            String eventType = event.getInteraction();
            String widgetType = wd.getSimpleType();
            int widgetId = wd.getId();
            int widgetIndex = wd.getIndex();
            String widgetName = wd.getName();
            String value = wd.getValue();

            testTrace.append(fireEvent.replaceFirst("<widgetId>", String.valueOf(widgetId))
                    .replaceFirst("<widgetIndex>", String.valueOf(widgetIndex))
                    .replaceFirst("<widgetName>", widgetName)
                    .replaceFirst("<widgetType>", widgetType)
                    .replaceFirst("<eventType>", eventType)
                    .replaceFirst("<value>", value));
        }

        if(new Random().nextBoolean()){
            testTrace.append(operationFactory.buildCall());
        }

//        testTrace.append(callPerturb.replaceFirst("<ARGS0>", args[0]));
        return testTrace.toString();
    }

    @Override
    public String recover(OperationFactory operationFactory) {
//        return callRecover.replaceFirst("<ARGS0>", args[0]);
        return null;
    }
}
