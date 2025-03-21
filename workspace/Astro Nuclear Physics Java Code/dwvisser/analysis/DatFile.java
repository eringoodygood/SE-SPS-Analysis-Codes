package dwvisser.analysis;
import java.io.File;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;

public class DatFile {

	private double[] data;

	public DatFile(File file) {
		readInFile(file);
	}
	
	public DatFile(File file, double [] outData){
		data = outData;
		writeFile(file);
	}
	
	public double [] getData(){
		return data;
	}

	private void readInFile(File file) {
		try {
			FileReader reader = new FileReader(file);
			StreamTokenizer st = new StreamTokenizer(reader);
			int maxCh = 0;
			st.nextToken(); //get channel
			do {
				int channel = (int) st.nval;
				if (channel > maxCh)
					maxCh = channel;
				st.nextToken(); //skip counts
				st.nextToken(); //get channel
			} while (st.ttype != StreamTokenizer.TT_EOF);
			reader.close();
			data = new double[maxCh + 1];
			reader = new FileReader(file);
			st = new StreamTokenizer(reader);
			st.nextToken(); //get channel
			do {
				int channel = (int) st.nval;
				st.nextToken(); //get counts
				data[channel] = st.nval;
				st.nextToken(); //get channel
			} while (st.ttype != StreamTokenizer.TT_EOF);
			reader.close();
		} catch (FileNotFoundException fnf) {
			System.err.println("File not found: " + fnf);
		} catch (IOException ioe) {
			System.err.println("Problem reading file: "+ioe);
		}
	}
	
	private void writeFile(File file){
		try {
			FileWriter writer =  new FileWriter(file);
			for (int channel=0; channel < data.length; channel++){
				writer.write(channel+"\t"+data[channel]+"\n");
			}
			writer.close();
		} catch (IOException ioe) {
			System.err.println("Problem writing file: "+ioe);
		}
	}
		
		
		

}