/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ParameterSetComponent extends JPanel implements ActionListener {
    
    private final JLabel lblParameters;
    private final JButton btnChange;
    
    private ParameterSet parameters;
    
    public ParameterSetComponent(final ParameterSet parameters) {
        
        this.parameters = parameters;
        
        this.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));
        
        lblParameters = new JLabel();
        this.add(lblParameters, BorderLayout.WEST);
        
        btnChange = new JButton("Change");
        btnChange.addActionListener(this);
        btnChange.setEnabled(true);
        
        //this.add(btnChange, 1, 0, 1, 1, 1, 0, GridBagConstraints.NONE);
        this.add(btnChange, BorderLayout.EAST);
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        Object src = event.getSource();
        
        if (src == btnChange) {
            
            if (parameters == null) return;
            
            ExitCode exitCode = parameters.showSetupDialog(null, true);
            if (exitCode != ExitCode.OK) return;
            
        }
        
        updateLabel();
    }
    
    public ParameterSet getValue() {
        return parameters;
    }
    
    public void setValue(final ParameterSet parameters) {
        this.parameters = parameters;
        
        updateLabel();
    }
    
    private void updateLabel() {
        // Update text for lblParameters
        String text = "<html>";
        for (final Parameter p : parameters.getParameters())
            text += p.getName() + " = " + p.getValue() + "<br>";
        
        text += "</html>";
        
        lblParameters.setText(text);
    }
}
