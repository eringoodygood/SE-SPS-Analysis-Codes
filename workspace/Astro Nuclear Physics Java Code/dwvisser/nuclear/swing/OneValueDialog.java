package dwvisser.nuclear.swing;

/**
 * Class to return a single double value to a double [] passed to it.
 *
 * @author Dale Visser
 * @version 1.0
 */
public class OneValueDialog extends javax.swing.JDialog implements ValuesDialog {
  /** A return status code - returned if Cancel button has been pressed */
  public static final int RET_CANCEL = 0;
  /** A return status code - returned if OK button has been pressed */
  public static final int RET_OK = 1;

  private double [] values;

  /** Initializes the Form */
  public OneValueDialog(String quantity, String units){
    super();
    setTitle("Enter a single "+quantity+"["+units+"]");
    initComponents();
    pack();
  }

  /** @return the return status of this dialog - one of RET_OK or RET_CANCEL */
  public int getReturnStatus () {
    return returnStatus;
  }

  public double [] getValues(){
    double [] temp = {0.0};
    return temp;
  }

  public void display(){
    show();
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the FormEditor.
   */
private void initComponents () {//GEN-BEGIN:initComponents
    buttonPanel = new javax.swing.JPanel ();
    okButton = new javax.swing.JButton ();
    cancelButton = new javax.swing.JButton ();
    tValue = new javax.swing.JTextField ();
    setModal (true);
    addWindowListener (new java.awt.event.WindowAdapter () {
        public void windowClosing (java.awt.event.WindowEvent evt) {
            closeDialog (evt);
        }
    }
    );

    buttonPanel.setLayout (new java.awt.FlowLayout (2, 5, 5));
    buttonPanel.setPreferredSize (new java.awt.Dimension(325, 37));
    buttonPanel.setMinimumSize (new java.awt.Dimension(500, 37));

      okButton.setText("OK");
      okButton.addActionListener (new java.awt.event.ActionListener () {
          public void actionPerformed (java.awt.event.ActionEvent evt) {
              okButtonActionPerformed (evt);
          }
      }
      );
  
      buttonPanel.add (okButton);
  
      cancelButton.setText("Cancel");
      cancelButton.addActionListener (new java.awt.event.ActionListener () {
          public void actionPerformed (java.awt.event.ActionEvent evt) {
              cancelButtonActionPerformed (evt);
          }
      }
      );
  
      buttonPanel.add (cancelButton);
  

    getContentPane ().add (buttonPanel, java.awt.BorderLayout.NORTH);

    tValue.setColumns (10);


    getContentPane ().add (tValue, java.awt.BorderLayout.WEST);

}//GEN-END:initComponents

  private void okButtonActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    doClose (RET_OK);
  }//GEN-LAST:event_okButtonActionPerformed

  private void cancelButtonActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    doClose (RET_CANCEL);
  }//GEN-LAST:event_cancelButtonActionPerformed

  /** Closes the dialog */
  private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
    doClose (RET_CANCEL);
  }//GEN-LAST:event_closeDialog

  private void doClose (int retStatus) {
    returnStatus = retStatus;
    if (returnStatus==RET_OK){
      double d=Double.parseDouble(tValue.getText().trim());
      values = new double[1];
      values[0]=d;
    } else {
      values=null;
    }
    setVisible (false);
    dispose ();
  }

// Variables declaration - do not modify//GEN-BEGIN:variables
private javax.swing.JPanel buttonPanel;
private javax.swing.JButton okButton;
private javax.swing.JButton cancelButton;
private javax.swing.JTextField tValue;
// End of variables declaration//GEN-END:variables

  private int returnStatus = RET_CANCEL;

  public static void main(java.lang.String[] args) {
    new OneValueDialog ("Beam Energy","MeV").show ();
  }

}
