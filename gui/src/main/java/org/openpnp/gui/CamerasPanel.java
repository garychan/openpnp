/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.gui.tablemodel.CamerasTableModel;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Feeder;

public class CamerasPanel extends JPanel implements WizardContainer {
	private final Frame frame;
	private final Configuration configuration;
	private final MachineControlsPanel machineControlsPanel;
	
	private JTable table;

	private CamerasTableModel tableModel;
	private TableRowSorter<CamerasTableModel> tableSorter;
	private JTextField searchTextField;
	JPanel configurationPanel;

	public CamerasPanel(Frame frame, Configuration configuration, MachineControlsPanel machineControlsPanel) {
		this.frame = frame;
		this.configuration = configuration;
		this.machineControlsPanel = machineControlsPanel;
		
		setLayout(new BorderLayout(0, 0));
		tableModel = new CamerasTableModel(configuration);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		panel.add(toolBar, BorderLayout.CENTER);
		
		JButton btnNewCamera = new JButton(newCameraAction);
		toolBar.add(btnNewCamera);
		
		JButton btnDeleteCamera = new JButton(deleteCameraAction);
		toolBar.add(btnDeleteCamera);
		
		toolBar.addSeparator();
		
		JButton btnTableScanner = new JButton(tableScannerAction);
		toolBar.add(btnTableScanner);
		
		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.EAST);

		JLabel lblSearch = new JLabel("Search");
		panel_1.add(lblSearch);

		searchTextField = new JTextField();
		searchTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				search();
			}
		});
		panel_1.add(searchTextField);
		searchTextField.setColumns(15);

		JComboBox lookingComboBox = new JComboBox(Looking.values());
		
		table = new JTable(tableModel);
		tableSorter = new TableRowSorter<CamerasTableModel>(tableModel);
		table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(lookingComboBox));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setDividerLocation(0.3);
		add(splitPane, BorderLayout.CENTER);
		
		configurationPanel = new JPanel();
		configurationPanel.setBorder(new TitledBorder(null, "Configuration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		splitPane.setLeftComponent(new JScrollPane(table));
		splitPane.setRightComponent(configurationPanel);
		configurationPanel.setLayout(new BorderLayout(0, 0));
		table.setRowSorter(tableSorter);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = table.getSelectedRow();
				
				configurationPanel.removeAll();
				if (index != -1) {
					index = table.convertRowIndexToModel(index);
					Camera camera = tableModel.getCamera(index);
					Wizard wizard = camera.getConfigurationWizard();
					if (wizard != null) {
						wizard.setWizardContainer(CamerasPanel.this);
						JPanel panel = wizard.getWizardPanel();
						configurationPanel.add(panel);
					}
				}
				
				revalidate();
				repaint();
			}
		});
	}

	private void search() {
		RowFilter<CamerasTableModel, Object> rf = null;
		// If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
		}
		catch (PatternSyntaxException e) {
			System.out.println(e);
			return;
		}
		tableSorter.setRowFilter(rf);
	}

	@Override
	public void wizardCompleted(Wizard wizard) {
		configuration.setDirty(true);
	}

	@Override
	public void wizardCancelled(Wizard wizard) {
	}
	
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public MachineControlsPanel getMachineControlsPanel() {
		return machineControlsPanel;
	}
	
	public Action newCameraAction = new AbstractAction("New Camera...") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ClassSelectionDialog<Camera> dialog = new ClassSelectionDialog<Camera>(
					JOptionPane.getFrameForComponent(CamerasPanel.this), 
					"Select Camera...", 
					"Please select a Camera implemention from the list below.", 
					configuration.getMachine().getCompatibleCameraClasses());
			dialog.setVisible(true);
			Class<? extends Camera> cameraClass = dialog.getSelectedClass();
			if (cameraClass == null) {
				return;
			}
			try {
				Camera camera = cameraClass.newInstance();
				configuration.resolve(camera);
				configuration.getMachine().addCamera(camera);
				tableModel.refresh();
				configuration.setDirty(true);
			}
			catch (Exception e) {
				MessageBoxes.errorBox(
						JOptionPane.getFrameForComponent(CamerasPanel.this), 
						"Camera Error", 
						e);
			}
		}
	};

	public Action deleteCameraAction = new AbstractAction("Delete Camera") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
//			configuration.getMachine().removeFeeder(getSelectedFeeder());
		}
	};
	
	
	private Action tableScannerAction = new AbstractAction("Table Scanner") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			TableScanner tableScanner = new TableScanner(frame, configuration);
			tableScanner.pack();
			tableScanner.setVisible(true);
		}
	};
}