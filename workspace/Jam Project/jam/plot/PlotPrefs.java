/*
 * Created on Jun 10, 2004
 */
package jam.plot;

import java.util.prefs.Preferences;

/**
 * Holds reference to the preferences node affecting the 
 * <code>jam.plot</code> package, as well as the preference names.
 * 
 * @author <a href="mailto:dale@visser.name">Dale Visser</a>
 * @version Jun 10, 2004
 * @see java.util.prefs.Preferences
 */
public interface PlotPrefs {
	final Preferences prefs=Preferences.userNodeForPackage(PlotPrefs.class);
	final String AUTO_IGNORE_ZERO="AutoIgnoreZero";
	final String AUTO_IGNORE_FULL="AutoIgnoreFull";
	final String BLACK_BACKGROUND="BlackBackground";
	final String AUTO_PEAK_FIND = "AutoPeakFind";
	final String SMOOTH_COLOR_SCALE="ContinuousColorScale";
	final String AUTO_ON_EXPAND="AutoOnExpand";
	final String HIGHLIGHT_GATE_CHANNELS="HighlightGatedChannels";
}
