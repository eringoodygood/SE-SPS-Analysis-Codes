/*
 * Created on May 7, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sourceforge.nukesim;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version May 7, 2004
 */
public final class PreviousFileMenuItems extends JMenu {
	
	private class PreviousFileOpenAction extends JMenuItem {
		private final File file;
		
		public PreviousFileOpenAction(File f) {
			super(f.getName());
			file=f;
			addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae) {
					fileOpener.openFile(file);
				}
			});
		}
		
		File getFile(){
			return file;
		}
	}
	
	
	final Preferences prefs;
	final FileOpener fileOpener;
	final int maxFiles;
	int nextAdd=0;
	
	public PreviousFileMenuItems(Class parent, FileOpener fo, JMenu fm, int max){
		super("Previous Files");
		final String name=parent.getName();
		final int lastDot=name.lastIndexOf('.');
		final String basicName=name.substring(lastDot+1);
		prefs=Preferences.userNodeForPackage(parent).node(basicName);
		fileOpener=fo;
		maxFiles=max;
		setPreviousFiles();
		fm.add(this);
	}
	
	public void setPreviousFiles(){
		for (int i=0; i<maxFiles; i++){
			final String iKey=PREV_FILE_KEY+i;
			final File f=new File(prefs.get(iKey,PREV_FILE_DEFAULT));
			if (f.exists()){
				addFileFromPrefs(f);
			} else {
				/* drop the rest of the list by one slot */
				for (int j=i+1; j<maxFiles;j++){
					final String lastKey=PREV_FILE_KEY+(j-1);
					final String thisKey=PREV_FILE_KEY+j;
					prefs.put(lastKey,prefs.get(thisKey,PREV_FILE_DEFAULT));
				}
			}
		}
	}
	
	private static final String PREV_FILE_KEY = "PreviousFile";
	private static final String PREV_FILE_DEFAULT = "null";
	
		
	private synchronized final void addFileFromPrefs(File f){
		add(new PreviousFileOpenAction(f));
		nextAdd++;
		nextAdd %= maxFiles;
	}
		
	public synchronized final void addPrevFile(File f){
		boolean add=true;
		for (int i=getMenuComponentCount()-1; i>=0; i--){
			final Component prev=getMenuComponent(i);
			if (prev instanceof PreviousFileOpenAction){
				final PreviousFileOpenAction pfoa=(PreviousFileOpenAction)prev;
				if (pfoa.getFile().getAbsolutePath().equals(f.getAbsolutePath())){
					add=false;
				}
			}
		}
		if(add) {
			add(new PreviousFileOpenAction(f));
			final String currentKey=PREV_FILE_KEY+nextAdd;
			final String newValue=f.getAbsolutePath();
			prefs.put(currentKey,newValue);			
		} 
	}	
}
