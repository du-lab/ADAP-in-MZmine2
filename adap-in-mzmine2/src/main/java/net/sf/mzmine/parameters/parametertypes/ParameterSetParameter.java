/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author aleksandrsmirnov
 */
public class ParameterSetParameter 
        implements UserParameter <ParameterSet, ParameterSetComponent> 
{
    private static Logger LOG = Logger.getLogger(MZmineCore.class.getName());
    
    private String name;
    private String description;
    private ParameterSet value;
 
    private static final String parameterElement = "parameter";
    private static final String nameAttribute = "name";
    
    public ParameterSetParameter() {
        this("", "", null);
    }
    
    public ParameterSetParameter(String name, String description,
            ParameterSet parameters) {
        this.name = name;
        this.description = description;
        this.value = parameters;
    }
    
    public ParameterSet getValue() {
        return value;
    }
    
    public void setValue(final ParameterSet parameters) {
        this.value = parameters;
    }
    
    @Override
    public String getName() {return this.name;}
    
    @Override
    public String getDescription() {return this.description;}
    
    @Override
    public ParameterSetParameter cloneParameter() {
        return new ParameterSetParameter(this.name, this.description, value);
    }
    
    @Override 
    public void setValueToComponent(final ParameterSetComponent component,
            final ParameterSet parameters) 
    {
        component.setValue(parameters);
    }
    
    @Override
    public void setValueFromComponent(final ParameterSetComponent component) {
        value = component.getValue();
    }
    
    @Override
    public ParameterSetComponent createEditingComponent() {
        return new ParameterSetComponent(this.value);
    }
    
    @Override
    public void saveValueToXML(Element xmlElement) {
        if (this.value == null) return;
        
        xmlElement.setAttribute("type", this.name);
        Document parent = xmlElement.getOwnerDocument();
        
        for (Parameter p : this.value.getParameters()) {
            Element newElement = parent.createElement(parameterElement);
            newElement.setAttribute(nameAttribute, p.getName());
            xmlElement.appendChild(newElement);
            p.saveValueToXML(newElement);
        }
    }
    
    @Override
    public void loadValueFromXML(Element xmlElement) {
        NodeList list = xmlElement.getElementsByTagName(parameterElement);
	for (int i = 0; i < list.getLength(); ++i) {
	    Element nextElement = (Element) list.item(i);
	    String paramName = nextElement.getAttribute(nameAttribute);
	    for (Parameter p : this.value.getParameters()) {
		if (p.getName().equals(paramName)) {
		    try {
			p.loadValueFromXML(nextElement);
		    } catch (Exception e) {
			LOG.log(Level.WARNING,
				"Error while loading parameter values for "
					+ p.getName(), e);
		    }
		}
	    }
	}
    }
    
    @Override
    public boolean checkValue(Collection <String> errorMessages) {
        
        boolean result = true;
        for (final Parameter p : this.value.getParameters())
            result &= p.checkValue(errorMessages);
        
        return result;
    }
}
