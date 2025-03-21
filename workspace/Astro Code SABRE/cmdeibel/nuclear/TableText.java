/*
 * Created on Dec 8, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package cmdeibel.nuclear;

public class TableText implements java.io.Serializable{
	private String name;
	private int colsSkip,colsME,colsUNC;
	private int year;
		
	public TableText(){
		//does nothing...only here for serializing
	}
	
	private TableText(String name, int colsSkip, int colsME, int colsUNC, int year){
		this.name=name;
		this.colsSkip=colsSkip;
		this.colsME=colsME;
		this.colsUNC=colsUNC;
		this.year=year;
	}
		
	public static final TableText TABLE_1995 = new TableText("mass_rmd.mas95",5,11,9,1995);
	public static final TableText TABLE_2003 = new TableText("mass.mas03",5,13,11,2003);
		
	public String getName(){
		return name;
	}
	public int getColsToSkip(){
		return colsSkip;
	}
	public int getColsMassExcess(){
		return colsME;
	}
	public int getColsUncertainty(){
		return colsUNC;
	}
	public int getYear(){
		return year;
	}
}
