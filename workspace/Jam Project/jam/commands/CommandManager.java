package jam.commands;

import jam.JamPrefs;
import jam.global.Broadcaster;
import jam.global.CommandListener;
import jam.global.CommandListenerException;
import jam.global.CommandNames;
import jam.global.JamStatus;
import jam.global.MessageHandler;
import jam.plot.PlotPrefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Action;

/**
 * Class to create commands and execute them
 *
 * @author Ken Swartz
 */
public class CommandManager implements CommandListener, CommandNames {

	private final JamStatus status=JamStatus.instance();
	private final MessageHandler msghdlr;
	private static CommandManager _instance=null;
	private static final Map cmdMap = Collections.synchronizedMap(new HashMap());
	private static final Map instances=Collections.synchronizedMap(new HashMap());
	private Commandable currentCommand;
	
	
	
	/* initializer block for map */
	static {
		cmdMap.put(OPEN_HDF, OpenHDFCmd.class);
		cmdMap.put(SAVE_HDF, SaveHDFCmd.class);
		cmdMap.put(SAVE_AS_HDF, SaveAsHDFCmd.class);
		cmdMap.put(SAVE_GATES, SaveGatesCmd.class);
		cmdMap.put(ADD_HDF, AddHDFCmd.class);
		cmdMap.put(RELOAD_HDF, ReloadHDFCmd.class);
		cmdMap.put(SHOW_NEW_HIST, ShowDialogNewHistogramCmd.class);
		cmdMap.put(SHOW_HIST_ZERO, ShowDialogZeroHistogram.class);
		
		cmdMap.put(SHOW_HIST_COMBINE, ShowDialogHistManipulationsCmd.class);
		cmdMap.put(SHOW_HIST_PROJECT, ShowDialogHistProjectionCmd.class);
		cmdMap.put(SHOW_HIST_FIT, ShowDialogCalibrationFitCmd.class);
		cmdMap.put(SHOW_HIST_DISPLAY_FIT, ShowDialogCalibrationDisplayCmd.class);
		cmdMap.put(SHOW_HIST_GAIN_SHIFT, ShowDialogGainShiftCmd.class);				 
				 
		cmdMap.put(SHOW_NEW_GATE, ShowDialogNewGateCmd.class);
		cmdMap.put(SHOW_SET_GATE, ShowDialogSetGate.class); 
		cmdMap.put(SHOW_ADD_GATE, ShowDialogAddGate.class); 
		cmdMap.put(SHOW_RUN_CONTROL, ShowRunControl.class); 
		cmdMap.put(SHOW_SORT_CONTROL, ShowSortControl.class); 
		cmdMap.put(START, StartAcquisition.class);
		cmdMap.put(STOP, StopAcquisition.class);
		cmdMap.put(FLUSH, FlushAcquisition.class);
		cmdMap.put(EXIT, ShowDialogExitCmd.class);
		cmdMap.put(NEW, FileNewClearCmd.class);
		cmdMap.put(PARAMETERS, ShowDialogParametersCmd.class);
		cmdMap.put(DISPLAY_SCALERS, ShowDialogScalersCmd.class);
		cmdMap.put(SHOW_ZERO_SCALERS, ShowDialogZeroScalersCmd.class);
		cmdMap.put(SCALERS, ScalersCmd.class);
		cmdMap.put(EXPORT_TEXT, ExportTextFileCmd.class);
		cmdMap.put(EXPORT_DAMM, ExportDamm.class);
		cmdMap.put(EXPORT_SPE, ExportRadware.class);	
		cmdMap.put(PRINT, Print.class);
		cmdMap.put(PAGE_SETUP, PageSetupCmd.class);	 
		cmdMap.put(IMPORT_TEXT, ImportTextFile.class);
		cmdMap.put(IMPORT_DAMM, ImportDamm.class);
		cmdMap.put(IMPORT_SPE, ImportRadware.class);
		cmdMap.put(IMPORT_XSYS, ImportXSYS.class);
		cmdMap.put(IMPORT_BAN, ImportORNLban.class);
		cmdMap.put(OPEN_SCALERS_YALE_CAEN, OpenScalersYaleCAEN.class);	
		cmdMap.put(SHOW_SCALER_SCAN, ShowDialogScalerScan.class);		
		cmdMap.put(DELETE_HISTOGRAM, DeleteHistogram.class);
		cmdMap.put(HELP_ABOUT, ShowDialogAbout.class);
		cmdMap.put(HELP_LICENSE, ShowDialogLicense.class);
		cmdMap.put(USER_GUIDE, ShowUserGuide.class);	
		cmdMap.put(OPEN_SELECTED, OpenSelectedHistogram.class);
		cmdMap.put(DISPLAY_MONITORS, ShowMonitorDisplay.class);
		cmdMap.put(DISPLAY_MON_CONFIG, ShowMonitorConfig.class);
		cmdMap.put(SHOW_BATCH_EXPORT, ShowBatchExport.class);
		cmdMap.put(SHOW_SETUP_ONLINE, ShowSetupOnline.class);
		cmdMap.put(SHOW_SETUP_OFFLINE, ShowSetupOffline.class);
		cmdMap.put(SHOW_BUFFER_COUNT, ShowDialogCounters.class);
		cmdMap.put(SHOW_FIT_NEW, ShowDialogAddFit.class);
		cmdMap.put(PlotPrefs.AUTO_IGNORE_ZERO, SetAutoScaleIgnoreZero.class);
		cmdMap.put(PlotPrefs.AUTO_IGNORE_FULL, SetAutoScaleIgnoreFull.class);
		cmdMap.put(PlotPrefs.BLACK_BACKGROUND, SetBlackBackground.class);
		cmdMap.put(PlotPrefs.AUTO_PEAK_FIND, SetAutoPeakFind.class);
		cmdMap.put(PlotPrefs.SMOOTH_COLOR_SCALE, 
		SetSmoothColorScale.class);
		cmdMap.put(PlotPrefs.AUTO_ON_EXPAND, SetAutoScaleOnExpand.class);
		cmdMap.put(PlotPrefs.HIGHLIGHT_GATE_CHANNELS, SetGatedChannelsHighlight.class);
		cmdMap.put(JamPrefs.VERBOSE, SetVerbose.class);
		cmdMap.put(JamPrefs.DEBUG, SetDebug.class);
		cmdMap.put(SHOW_PEAK_FIND, ShowDialogPeakFind.class);
		cmdMap.put(SHOW_SETUP_REMOTE, ShowSetupRemote.class);
	}
	

	/**
	 * Constructor private as singleton
	 *
	 */
	private CommandManager() {
		msghdlr=status.getMessageHandler();
	}
	
	/**
	 * Singleton accessor.
	 * 
	 * @return the unique instance of this class
	 */
	public static CommandManager getInstance () {
		if (_instance==null) {
			_instance=new CommandManager();
		}		
		return _instance;
	}
	
	/**
	 * Perform command with object parameters
	 *
	 * @param strCmd	String key indicating the command
	 * @param cmdParams	Command parameters
	 */
	/*private boolean performCommand(String strCmd, Object[] cmdParams)
		throws CommandException {
		boolean validCommand=false;
		if (createCmd(strCmd)) {
			if (currentCommand.isEnabled()){
				currentCommand.performCommand(cmdParams);
			} else {
				msghdlr.errorOutln("Disabled command \""+strCmd+"\"");
			}				
			validCommand= true;
		}
		return validCommand;
	}*/

	/**
	 * Perform command with string parameters
	 *
	 * @param strCmd 		String key indicating the command
	 * @param strCmdParams  Command parameters as strings
	 */
	public boolean performParseCommand(String strCmd, String[] strCmdParams) {
		boolean validCommand=false;
		if (createCmd(strCmd)) {
			if (currentCommand.isEnabled()){
				try{
					currentCommand.performParseCommand(strCmdParams);
				} catch (CommandListenerException cle){
					msghdlr.errorOutln("Performing command "+strCmd+"; "+
					cle.getMessage());
				}
			} else {
				msghdlr.errorOutln("Disabled command \""+strCmd+"\"");
			}
			validCommand=true;
		} 
		return validCommand;
	}
	
	/**
	 * See if we have the instance created, create it if necessary,
	 * and return whether it was successfully created. 
	 * 
	 * @param strCmd name of the command
	 * @return <code>true</code> if successful, <code>false</code> if 
	 * the given command doesn't exist
	 */
	private boolean createCmd(String strCmd)  {
		final boolean exists=cmdMap.containsKey(strCmd);
		if (exists) {
			final Class cmdClass = (Class)cmdMap.get(strCmd);
			currentCommand = null;
			final boolean created=instances.containsKey(strCmd);
			if (created){
				currentCommand=(Commandable) instances.get(strCmd);
			} else {
				try {
					currentCommand = (Commandable) (cmdClass.newInstance());
					currentCommand.initCommand();
					if (currentCommand instanceof Observer){
						Broadcaster.getSingletonInstance().addObserver(
						(Observer)currentCommand);
					}
				} catch (Exception e) {
					/* There was a problem resolving the command class or 
					 * with creating an instance. This should never happen
					 * if exists==true. */
					throw new RuntimeException(e);
				}
				instances.put(strCmd,currentCommand);
			}
		}
		return exists;
	}
	
	public Action getAction(String strCmd){
		return createCmd(strCmd) ? currentCommand : null;
	}
	
	public void setEnabled(String cmd, boolean enable){
		getAction(cmd).setEnabled(enable);
	}
	
	public String [] getSimilarCommnands(final String s, boolean onlyEnabled){
		final SortedSet sim=new TreeSet();
		final Set keys=cmdMap.keySet();
		for (int i=s.length(); i>=1; i--){
			final String com=s.substring(0,i);
			for (Iterator it=keys.iterator(); it.hasNext();){
				final String key=(String)it.next();
				if (key.startsWith(com)){
					final boolean addIt=(!onlyEnabled) || 
					getAction(key).isEnabled();
					if (addIt){
						sim.add(key);
					}
				}
			}
			if (!sim.isEmpty()){
				break;
			}
		}
		final String [] rval=new String[sim.size()];
		int i=0;
		for (Iterator it=sim.iterator(); it.hasNext(); i++){
			rval[i]=(String)it.next();
		}
		return rval;
	}
	
	public String [] getAllCommands(){
		final Object [] c=cmdMap.keySet().toArray();
		final String [] rval=new String[c.length];
		System.arraycopy(c,0,rval,0,c.length);
		return rval;
	}
}
