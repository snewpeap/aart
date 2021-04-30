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

package it.unina.android.shared.ripper.model.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Abstraction of a GUI Widget, an Android GUI Component
 * 
 * @author Nicola Amatucci - REvERSE
 *
 */
public class WidgetDescription implements Serializable
{
	Integer id;
	Class<?> type;
	String className;
	HashMap<String, Boolean> listeners;
	ArrayList<String> supportedEvents;
	Integer textType;
	String simpleType;
	String name;
	Boolean enabled;
	Boolean visible;
	String value;
	public static final int NO_COUNT = -1;
	Integer count = NO_COUNT;
	String textualId;
	Integer index;
	Integer parentId;
	String parentName;
	String parentType;
	Integer ancestorId; //first ancestor with id set
	String ancestorType; //first ancestor with id set
	public static final int NO_PARENT_INDEX = Integer.MIN_VALUE;
	Integer parentIndex = NO_PARENT_INDEX;
	Integer depth = -1;

	//TODO: riccio
	Integer scrollViewX;
	Integer scrollViewY;
	
	public WidgetDescription()
	{
		super();
		this.listeners = new HashMap<>();
		this.supportedEvents = new ArrayList<>();
		this.enabled = true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WidgetDescription) {
			WidgetDescription wd = (WidgetDescription) obj;
			return id != null && id.equals(wd.id) &&
					simpleType != null && simpleType.equals(wd.simpleType) &&
					Objects.equals(name, wd.name) &&
					Objects.equals(textualId, wd.textualId) &&
					Objects.equals(parentIndex, wd.parentIndex) &&
					capabilitiesEquals(wd);
		} else
			return false;
	}

	public boolean capabilitiesEquals(WidgetDescription wd) {
		return Objects.equals(judgeClickable(), wd.judgeClickable()) &&
				Objects.equals(judgeLongClickable(), wd.judgeLongClickable()) &&
				Objects.equals(enabled, wd.enabled) &&
				Objects.equals(visible, wd.visible);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, simpleType, name, textualId, enabled, visible, judgeClickable(), judgeLongClickable());
	}

	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = Integer.parseInt(id);
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public void setType(Class<?> type) {
		this.type = type;
	}
	
	public HashMap<String, Boolean> getListeners() {
		return listeners;
	}

	public void setListeners(HashMap<String, Boolean> listeners) {
		this.listeners = listeners;
	}
	
	public void addListener(String key, Boolean value)
	{
		this.listeners.put(key, value);
	}
	
	public ArrayList<String> getSupportedEvents() {
		return supportedEvents;
	}

	public void setSupportedEvents(ArrayList<String> supportedEvents) {
		this.supportedEvents = supportedEvents;
	}

	public void addSupportedEvent(String key)
	{
		this.supportedEvents.add(key);
	}
	
	public Integer getTextType() {
		return textType;
	}

	public void setTextType(Integer textType) {
		this.textType = textType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public String getTextualId() {
		return textualId;
	}

	public void setTextualId(String textualId) {
		this.textualId = textualId;
	}
	
	public String getSimpleType() {
		return simpleType;
	}

	public void setSimpleType(String simpleType) {
		this.simpleType = simpleType;
	}
	
	public String getClassName() {
		if (this.type != null) {
			return this.type.getCanonicalName();
		} else {
			return className;
		}
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	private Boolean clickable;
	private Boolean longClickable;

	public Boolean getClickable() {
		return clickable;
	}

	public void setClickable(Boolean clickable) {
		this.clickable = clickable;
	}

	public Boolean getLongClickable() {
		return longClickable;
	}

	public void setLongClickable(Boolean longClickable) {
		this.longClickable = longClickable;
	}

	public boolean judgeClickable() {
		return clickable || isListenerActive("OnClickListener");
	}


	public boolean judgeLongClickable()
	{
		return longClickable || isListenerActive("OnLongClickListener");
	}

	public boolean hasOnFocusChangeListener()
	{
		return (isListenerActive("OnFocusChangeListener"));
	}
	
	public boolean hasOnKeyListener()
	{
		return (isListenerActive("OnKeyListener"));
	}
	
	public boolean hasListener(String listenerName)
	{
		return listeners.containsKey(listenerName);
	}
	
	public boolean isListenerActive(String listenerName)
	{
		return hasListener(listenerName) && listeners.get(listenerName);
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}
	
	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
	
	public String getParentType() {
		return parentType;
	}

	public void setParentType(String parentType) {
		this.parentType = parentType;
	}
	
	public Integer getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(Integer ancestorId) {
		this.ancestorId = ancestorId;
	}
	
	public String getAncestorType() {
		return ancestorType;
	}

	public void setAncestorType(String ancestorType) {
		this.ancestorType = ancestorType;
	}
	
	public Boolean isVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}

	public Integer getParentIndex() {
		return parentIndex;
	}

	public void setParentIndex(Integer parentIndex) {
		this.parentIndex = parentIndex;
	}

	public Integer getDepth() {
		return depth;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public Boolean getVisible() {
		return visible;
	}

	public Integer getScrollViewX() {
		return scrollViewX;
	}

	public void setScrollViewX(Integer scrollViewX) {
		this.scrollViewX = scrollViewX;
	}

	public Integer getScrollViewY() {
		return scrollViewY;
	}

	public void setScrollViewY(Integer scrollViewY) {
		this.scrollViewY = scrollViewY;
	}
	
	@Override
	public String toString()
	{
		/*return 	"[id="+this.id+"]" +
				"[type="+((this.type!=null)?this.type.getCanonicalName():className)+"]";*/
		return String.format("[p_idx=%d, idx=%d, id=%d]%s", parentIndex, index, id, getClassName());
	}
	
	public String toXMLString()
	{
		String xml = "";
		
		xml += "<widget " +					
					"id=\""+this.id+"\" " +
					"type=\""+((this.type!=null)?this.type.getCanonicalName():className)+"\" " +
				" />";
		
		return xml;
	}
}
