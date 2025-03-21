/*
 * Created on Mar 5, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package dwvisser.nuclear.swing;
import javax.swing.*;
import java.awt.event.*;
import dwvisser.nuclear.*;

/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Mar 5, 2004
 */
public class MassTableChooserMenuItems {
	public MassTableChooserMenuItems(JMenu addto, ItemListener il){
		final JRadioButtonMenuItem m1995 =
			new JRadioButtonMenuItem("1995 Mass Table");
		m1995.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				if (ie.getStateChange() == ItemEvent.SELECTED) {
					Nucleus.setMassTable(TableText.TABLE_1995);
				}
			}
		});
		m1995.addItemListener(il);
		final JRadioButtonMenuItem m2003 =
			new JRadioButtonMenuItem("2003 Mass Table");
		m2003.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				if (ie.getStateChange() == ItemEvent.SELECTED) {
					Nucleus.setMassTable(TableText.TABLE_2003);
				}
			}
		});
		m2003.addItemListener(il);
		final ButtonGroup choice = new ButtonGroup();
		choice.add(m1995);
		choice.add(m2003);
		m2003.setSelected(true);
		addto.add(m1995);
		addto.add(m2003);
	}
}
