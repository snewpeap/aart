/**
 * GNU Affero General Public License, version 3
 *
 * Copyright (c) 2014-2017 REvERSE, REsEarch gRoup of Software Engineering @ the University of Naples Federico II, http://reverse.dieti.unina.it/
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/

package it.unina.android.shared.ripper.model.transition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import android.ripper.extension.robustness.tools.ObjectTool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unina.android.shared.ripper.model.state.WidgetDescription;

/**
 * Event
 *
 * @author Nicola Amatucci - REvERSE
 *
 */
public class Event implements Serializable, IEvent {

	private long eventUID = 0;
	private String interaction;
	private WidgetDescription widget;
	private String value;
	private ArrayList<Input> inputs;

	private String beforeExecutionStateUID = "UNDEFINED";
	private String afterExecutionStateUID = "UNDEFINED";
	private int idle = 0;
	@JsonIgnore
	public static final int IDLE_SIZE = 2000;

	public void updateIdle(int newIdle) {
		idle = Integer.max(idle, newIdle);
	}

	public int getIdle() {
		return idle;
	}

	public void setIdle(int idle) {
		this.idle = idle;
	}


	public Event() {
		super();
	}

	public Event(String interaction) {
		this(interaction, null, null, null);
	}

	public Event(String interaction, WidgetDescription widget, String value) {
		this.interaction = interaction;
		this.widget = widget;
		this.value = value;
		this.inputs = null;
	}

	public Event(String interaction, WidgetDescription widget, String value, ArrayList<Input> inputs) {
		this.interaction = interaction;
		this.widget = widget;
		this.value = value;
		this.inputs = inputs;
	}

	public String getInteraction() {
		return interaction;
	}

	public boolean is(String interaction) {
		return Objects.equals(this.getInteraction(), interaction);
	}

	public void setInteraction(String interaction) {
		this.interaction = interaction;
	}

	public WidgetDescription getWidget() {
		return widget;
	}

	public void setWidget(WidgetDescription widget) {
		this.widget = widget;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ArrayList<Input> getInputs() {
		return inputs;
	}

	public void setInputs(ArrayList<Input> inputs) {
		this.inputs = inputs;
	}

	public long getEventUID() {
		return eventUID;
	}

	public void setEventUID(long eventUID) {
		this.eventUID = eventUID;
	}

	public void addInput(WidgetDescription widget, String interactionType, String value)
	{
		if (inputs == null) {
			inputs = new ArrayList<>();
		}
		this.inputs.add(new Input(widget, interactionType, value));
	}

	public void clearInputs()
	{
		this.inputs.clear();
	}

	public String getBeforeExecutionStateUID() {
		return beforeExecutionStateUID;
	}

	public void setBeforeExecutionStateUID(String beforeExecutionStateUID) {
		this.beforeExecutionStateUID = beforeExecutionStateUID;
	}

	public String getAfterExecutionStateUID() {
		return afterExecutionStateUID;
	}

	public void setAfterExecutionStateUID(String afterExecutionStateUID) {
		this.afterExecutionStateUID = afterExecutionStateUID;
	}

	@Override
	public String toString() {
		return String.format("%s%s, idle %d ms", (widget != null) ? widget + "." : "", interaction, idle);
	}

	public String toXMLString() {
		StringBuilder xml = new StringBuilder();
		xml.append(String.format("<event interaction=\"%s\" value=\"%s\" >\n", interaction, value));

		if (widget != null)
			xml.append(widget.toXMLString());
		else
			xml.append("<widget id=\"null\" type=\"null\" />\n");

		if (this.inputs != null && this.inputs.size() > 0)
			for (Input in : this.inputs)
				xml.append(in.toXMLString());

		xml.append("</event>\n");

		return xml.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Event) {
			Event e = (Event) o;
			boolean equals;
			if (inputs == null && e.inputs == null) {
				equals = Objects.equals(widget, e.widget) &&
						Objects.equals(interaction, e.interaction) &&
						Objects.equals(value, e.value);
			} else if (equals = inputs != null && e.inputs != null) {
				if (equals = inputs.size() == e.inputs.size()) {
					for (int i = 0; i < inputs.size(); i++) {
						Input thisInput = inputs.get(i), thatInput = e.inputs.get(i);
						equals = Objects.equals(thisInput.getInputType(), thatInput.getInputType()) &&
								Objects.equals(thisInput.getWidget(), thatInput.getWidget()) &&
								Objects.equals(thisInput.getValue(), thatInput.getValue());
					}
				}
			}
			return equals;
		}
		return false;
	}
}
