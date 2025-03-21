package jam.commands;

import jam.global.CommandListenerException;
import jam.global.JamProperties;
import jam.util.YaleCAENgetScalers;

import java.io.File;
import javax.swing.JFileChooser;
import jam.io.ExtensionFileFilter;


/**
 * Open a file with YaleCAEN scalers.
 * 
 * @author Ken Swartz
 */
final class OpenScalersYaleCAEN extends AbstractCommand {

	private YaleCAENgetScalers ycs;
	
	public void initCommand(){
		putValue(NAME,"Display scalers from YaleCAEN event file\u2026");
		ycs= new YaleCAENgetScalers();						
	}

	protected void execute(Object[] cmdParams) {		
		final File file;
		if (cmdParams==null) {
			file = getFile();					
		} else {
			file =(File)cmdParams[0];
		}
			
		if (file != null) {
			ycs.processEventFile(file);
		}	
	}

	protected void executeParse(String[] cmdTokens)
		throws CommandListenerException {
		execute(null);
	}

	/**
	 * Get a *.evn file from a JFileChooser.
	 *
	 * @return	a <code>File</code> chosen by the user, null if 
	 * dialog cancelled
	 */
	private File getFile() {
		
		File lastFile =new File(JamProperties.getPropString(JamProperties.EVENT_INPATH));
		
		File file = null;
		int option;
		JFileChooser jfile = new JFileChooser(lastFile);
		jfile.setDialogTitle("Select an Event File");
		jfile.setFileFilter(new ExtensionFileFilter("evn"));
		option = jfile.showOpenDialog(null);
		/* don't do anything if it was cancel */
		if (option == JFileChooser.APPROVE_OPTION) {
			file = jfile.getSelectedFile();
			lastFile = file;
		}
		return file;
	}
}
