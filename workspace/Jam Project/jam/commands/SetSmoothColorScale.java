/*
 * Created on Jun 11, 2004
 *
 */
package jam.commands;
import jam.plot.PlotPrefs;

/**
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Jun 11, 2004
 */
final class SetSmoothColorScale extends AbstractSetBooleanPreference {

	SetSmoothColorScale(){
		super();
		putValue(NAME, "Use gradient color scale");
		putValue(SHORT_DESCRIPTION,
		"Use a continuous rainbow color scale for 2d histograms.");
		prefsNode=PlotPrefs.prefs;
		key=PlotPrefs.SMOOTH_COLOR_SCALE;
		defaultState=false;
	}
}
