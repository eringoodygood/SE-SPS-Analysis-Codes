/*
 * Created on Mar 5, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sourceforge.nukesim.nuclear.swing;
import javax.swing.*;
import java.awt.event.*;
import net.sourceforge.nukesim.nuclear.*;
import java.util.*;

/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Mar 5, 2004
 */
public class MassTableChooserMenuItems {
	
	private final JRadioButtonMenuItem m1995 =
	new JRadioButtonMenuItem("1995 Mass Table");
	private final JRadioButtonMenuItem m2003 =
	new JRadioButtonMenuItem("2003 Mass Table");
	private final List listeners=new ArrayList();
	
	public MassTableChooserMenuItems(JMenu addto, MassChangeListener il){
		m1995.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				if (ie.getStateChange() == ItemEvent.SELECTED) {
					Nucleus.setMassTable(TableText.TABLE_1995);
					massesChanged();
				}
			}
		});
		m2003.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				if (ie.getStateChange() == ItemEvent.SELECTED) {
					Nucleus.setMassTable(TableText.TABLE_2003);
					massesChanged();
				}
			}
		});
		final ButtonGroup choice = new ButtonGroup();
		choice.add(m1995);
		choice.add(m2003);
		m2003.setSelected(true);
		addto.add(m1995);
		addto.add(m2003);
		addListener(il);
	}
	
	public final void addListener(MassChangeListener l){
		listeners.add(l);
	}
	
	private void massesChanged(){
		for (Iterator it=listeners.iterator(); it.hasNext();){
			final MassChangeListener mcl=(MassChangeListener)it.next();
			mcl.massChanged();
		}
	}
}
