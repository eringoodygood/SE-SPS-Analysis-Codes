package jam.commands;

import jam.global.CommandListenerException;

import javax.swing.JDialog;


/**
 * Commands that are for showing <code>JDialog</code>'s. Dialogs
 * simply extend this and assign a reference to 
 * <code>dialog</control> in <code>initCommand()</code>.
 * 
 * @author Ken Swartz
 */
public class AbstractShowDialog
	extends AbstractCommand {

	/**
	 * Dialog to show.
	 */
	protected JDialog dialog;
	
	protected final void execute(Object[] cmdParams) {
		dialog.show();
	}

	protected final void executeParse(String[] cmdTokens)
		throws CommandListenerException {
		execute(null);
	}
	
	/**
	 * Executes superclass's method of the same name, then disposes
	 * the dialog if its show command is disabled.
	 * 
	 * @see javax.swing.Action#setEnabled(boolean)
	 */
	public final void setEnabled(boolean state){
		super.setEnabled(state);
		if (!state){
			dialog.dispose();
		}
	}
}
