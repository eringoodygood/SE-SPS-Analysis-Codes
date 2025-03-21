/*
 * ComponentPrintable.java
 *
 * Created on October 24, 2001, 11:28 AM
 */

package dwvisser.nuclear.swing;
import javax.swing.JComponent;
import java.awt.*;
import java.awt.print.*;

/**
 * Class copied from "Java 2D Graphics" by J. Knudsen, wraps AWT & Swing 
 * Componenents in order to render them on a printer.
 *
 * @author  dwvisser
 * @version 
 */
public class ComponentPrintable implements Printable {

    private Component mComponent;
    /** Creates new ComponentPrintable */
    public ComponentPrintable(Component c) {
        mComponent = c;
    }

    public int print(java.awt.Graphics g, PageFormat pageFormat, int pageIndex) throws java.awt.print.PrinterException {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        Graphics2D g2 = (Graphics2D)g;
        g2.translate(pageFormat.getImageableX(),pageFormat.getImageableY());
        boolean wasBuffered = disableDoubleBuffering(mComponent);
        mComponent.paint(g2);
        restoreDoubleBuffering(mComponent, wasBuffered);
        return PAGE_EXISTS;
    }
    
    private boolean disableDoubleBuffering(Component c){
        if (!(c instanceof JComponent)) return false;
        JComponent jc=(JComponent)c;
        boolean wasBuffered = jc.isDoubleBuffered();
        jc.setDoubleBuffered(false);
        return wasBuffered;
    }
    
    private void restoreDoubleBuffering(Component c, boolean wasBuffered) {
        if (c instanceof JComponent)
            ((JComponent)c).setDoubleBuffered(wasBuffered);
    }
       
}
