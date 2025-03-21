package dwvisser.jamUtilities;
import java.io.*;
public class GetEventScalers {

	/**
	 * This program reads through and event file and produces a csv file containing the numbers
	 * stored in scaler blocks.
	 * 
	 * @param args event file to read
	 */
	public static void main(String[] args) {
		final int SCALER_HEADER = 0x01cccccc;

		DataInputStream fromStream = null;
		FileWriter csvStream = null;
		FileInputStream fromFile;
		File csvFile;
		if (args.length != 0) {
			String fromFileName = args[0];
			String csvFileName =
				fromFileName.substring(0, fromFileName.lastIndexOf(".evn"))
					+ "_scalers.csv";

			System.out.println("Reading file: " + fromFileName);
			System.out.println("Scaler summary in: " + csvFileName);

			try {
				fromFile = new FileInputStream(args[0]);
				csvFile = new File(csvFileName);
				fromStream =
					new DataInputStream(new BufferedInputStream(fromFile));
				csvStream = new FileWriter(csvFile);

				//skip header from input stream 
				fromStream.skipBytes(256);

				int blockNum = 0;
				while (true) {
					int read_val = fromStream.readInt();
					if (read_val == SCALER_HEADER) {
						blockNum++;
						int numScalers = fromStream.readInt();
						System.out.println(
							"Scaler block #"
								+ blockNum
								+ ", "
								+ numScalers
								+ " scaler values.");
						for (int i = 1; i <= numScalers; i++) {
							csvStream.write(
								Integer.toString(fromStream.readInt()));
							if (i < numScalers)
								csvStream.write(",");
						}
						csvStream.write("\n");
					}
				}
			} catch (EOFException e) {
				System.out.println("End of event file reached. Closing files.");
			} catch (IOException e) {
				System.err.println(e);
			}

			try {
				fromStream.close();
				csvStream.flush();
				csvStream.close();
			} catch (IOException e) {
				System.err.println(e);
			}

			System.out.println("Done.");
		} else {
			System.out.println("You need to specify an event file.");
		}
	}
}
