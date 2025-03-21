package sort;
import jam.data.*;
import jam.sort.*;

/*
 * Test sort file for ADC at LENA.
 *
 * @author C. Fox
 * @author C. Iliadis
 */
public class Test extends SortRoutine {
	static final int ADC_BASE = 0xe0000000;
	static final int SCALER_ADDRESS = 0xe100000;
	static final int TDC_BASE = 0xe0010000;
	static final int THRESHOLDS = 0;
	static final int TIME_RANGE = 1200;
	//	static final int CHRIS_ADC = 0xe00001090;

	final int ADC_CHANNELS = 4096; //num of channels per ADC
	final int COMPRESSED_CHANNELS = 512;
	//number of channels in compressed position histogram
	final int TWO_D_CHANNELS = 256;
	//number of channels per dimension in 2-d histograms

	//amount of bits to shift for compression

	final int COMPRESS_FACTOR =
		Math.round(
			(float) (Math.log(ADC_CHANNELS / COMPRESSED_CHANNELS)
				/ Math.log(2.0)));
	final int TWO_D_FACTOR =
		Math.round(
			(float) (Math.log(ADC_CHANNELS / TWO_D_CHANNELS) / Math.log(2.0)));

	//  id numbers for the signals

	int idGe;
	int idNaI;
	int idTAC_Coin;
	int idTAC_Anti;
	int idEmpty;
	int idTDC;

	//  ungated 1D spectra

	Histogram hGe;
	Histogram hNaI;
	Histogram hTAC_Coin;
	Histogram hTAC_Anti;
	Histogram hTDC;
	Histogram hEmpty;

	//  ungated 2D spectra

	Histogram hGeNaI;

	//  gates in TACB (for anticoncidence)

	Histogram hGe_TAC_Anti1;
	Histogram hGe_TAC_Anti2;

	//  gates in NaI ( For threshold )

	Histogram hAC_short_thresh;
	Histogram hAC_long_thresh;

	//  gates in Ge vs. NaI

	Histogram hGe_1;
	Histogram hGe_2;
	Histogram hGe_3;
	Histogram hGe_4;
	Histogram hGe_5;
	Histogram hGe_6;

	//  gates

	Gate gTAC_Coin1;
	Gate gTAC_Coin2;
	Gate gTAC_Anti1;
	Gate gTAC_Anti2;
	Gate gGeNaIa;
	Gate gGeNaIb;
	Gate gGeNaIc;
	Gate gAC_long_thresh;
	Gate gAC_short_thresh;

	//  scalers

	Scaler sBCI; // new scaler cf
	Scaler sClock;
	Scaler sBusyVito; // new cf
	Scaler s4;
	Scaler s5;
	Scaler s6;
	Scaler s7;
	Scaler s8;
	public void initialize() throws Exception {
		vmeMap.setScalerInterval(3);
		vmeMap.setV775Range(TDC_BASE, TIME_RANGE);
		// eventParameters, args = (slot, base address, channel, threshold channel)
		idGe = vmeMap.eventParameter(2, ADC_BASE, 0, 0);
		idNaI = vmeMap.eventParameter(2, ADC_BASE, 5, 0);
		idEmpty = vmeMap.eventParameter(8, TDC_BASE, 15, 0);
		idTAC_Coin = vmeMap.eventParameter(2, ADC_BASE, 1, 0);
		idTAC_Anti = vmeMap.eventParameter(2, ADC_BASE, 2, 0);
		//	idEmpty = vmeMap.eventParameter(2, 0x1090, 5, 256);
		idTDC = vmeMap.eventParameter(8, TDC_BASE, 1, 0);

		hGe =
			new Histogram(
				"Ge-Singles",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge Singles");

		hGe_TAC_Anti1 =
			new Histogram(
				"Ge-AC-short",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge AntiCoincidence short gate (5us)");
		hAC_short_thresh =
			new Histogram(
				"Ge-AC-short-th",
				HIST_1D_INT,
				ADC_CHANNELS,
				"AntiCoincidnce short gate NaI threshold");

		hGe_TAC_Anti2 =
			new Histogram(
				"Ge-AC-long",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge AntiCoincidence long gate (10us)");
		hAC_long_thresh =
			new Histogram(
				"Ge-AC-long-th",
				HIST_1D_INT,
				ADC_CHANNELS,
				"AntiCoincidence long gate NaI threshold");
		hGe_1 =
			new Histogram(
				"Ge-TAC-Coin1NaIa",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin1NaIa");
		hGe_2 =
			new Histogram(
				"Ge-TAC-Coin1NaIb",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin1NaIb");
		hGe_3 =
			new Histogram(
				"Ge-TAC-Coin1NaIc",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin1NaIc");
		hGe_4 =
			new Histogram(
				"Ge-TAC-Coin2NaIa",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin2NaIa");
		hGe_5 =
			new Histogram(
				"Ge-TAC-Coin2NaIb",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin2NaIb");
		hGe_6 =
			new Histogram(
				"Ge-TAC-Coin2NaIc",
				HIST_1D_INT,
				ADC_CHANNELS,
				"Ge-TAC-Coin2NaIc");
		hTAC_Coin =
			new Histogram(
				"TAC-Coincidence",
				HIST_1D_INT,
				ADC_CHANNELS,
				"TAC_Coincidence");
		hTAC_Anti =
			new Histogram(
				"TAC-AntiCoincidence",
				HIST_1D_INT,
				ADC_CHANNELS,
				"TAC_AntiCoincidence");
		hNaI = new Histogram("NaI", HIST_1D_INT, ADC_CHANNELS, "NaI");

		hGeNaI =
			new Histogram("GeNaI-2d", HIST_2D_INT, TWO_D_CHANNELS, "Ge vs NaI");
		hTDC = new Histogram("TDC", HIST_1D_INT, ADC_CHANNELS, "TDC");
		hEmpty = new Histogram("Empty", HIST_1D_INT, ADC_CHANNELS, "Empty");

		gTAC_Coin1 = new Gate("TAC-Coincidence1", hTAC_Coin);
		gTAC_Coin2 = new Gate("TAC-Coincidence2", hTAC_Coin);
		gTAC_Anti1 = new Gate("TAC-AntiCoincidence1", hTAC_Anti);
		gTAC_Anti2 = new Gate("TAC-AntiCoincidence2", hTAC_Anti);

		gAC_short_thresh = new Gate("AC-short-th", hNaI);
		gAC_long_thresh = new Gate("AC-long-th", hNaI);

		gGeNaIa = new Gate("Ge vs NaI a", hGeNaI);
		gGeNaIb = new Gate("Ge vs NaI b", hGeNaI);
		gGeNaIc = new Gate("Ge vs NaI c", hGeNaI);

		sBCI = new Scaler("BCI", 0); // scaler cf
		sClock = new Scaler("Clock", 1);
		sBusyVito = new Scaler("Busy Vito", 2); // new cf
		s4 = new Scaler("Scaler 4", 3);
		s5 = new Scaler("Scaler 5", 4);
		s6 = new Scaler("Scaler 6", 5);
		s7 = new Scaler("Scaler 7", 6);
		s8 = new Scaler("Scaler 8", 7);
		//	vmeMap.scalerParameter( 2048+sPulser.getNumber(), SCALER_ADDRESS,
		//				sPulser.getNumber(), sPulser);

		//	Scaler sPulser = new Scaler("Pulser",0);

		System.err.println("# Parameters: " + getEventSize());
		System.err.println("ADC channels: " + ADC_CHANNELS);
		System.err.println(
			"2d channels: "
				+ TWO_D_CHANNELS
				+ ", compression factor: "
				+ TWO_D_FACTOR);
		System.err.println(
			"compressed channels: "
				+ COMPRESSED_CHANNELS
				+ ", compression factor: "
				+ COMPRESS_FACTOR);
	}

	public void sort(int[] data) throws Exception {

		int eGe = data[idGe];
		int eNaI = data[idNaI];
		int eTAC_Coin = data[idTAC_Coin];
		int eTAC_Anti = data[idTAC_Anti];
		int eTDC = data[idTDC];
		int eEmpty = data[idEmpty];

		int ecGe = eGe >> TWO_D_FACTOR;
		int ecNaI = eNaI >> TWO_D_FACTOR;
		int ecTAC_Coin = eTAC_Coin >> TWO_D_FACTOR;
		int ecTAC_Anti = eTAC_Anti >> TWO_D_FACTOR;

		//      singles 1D spectra

		hGe.inc(eGe);
		hNaI.inc(eNaI);
		hTAC_Coin.inc(eTAC_Coin);
		hTAC_Anti.inc(eTAC_Anti);
		hTDC.inc(eTDC);
		hEmpty.inc(eEmpty);

		//      singles 2D spectra

		hGeNaI.inc(ecGe, ecNaI);

		//      gate on TAC

		// if (gTAC1.inGate(eTAC)){
		// hGe_TAC1.inc(eGe);
		// }

		if (!gTAC_Anti1.inGate(eTAC_Anti)) {
			hGe_TAC_Anti1.inc(eGe);
		}

		if (!gTAC_Anti2.inGate(eTAC_Anti)) {
			hGe_TAC_Anti2.inc(eGe);
		}

		//      gate on Ge vs NaI

		if (gGeNaIa.inGate(ecGe, ecNaI) && gTAC_Coin1.inGate(eTAC_Coin)) {
			hGe_1.inc(eGe);
		}
		if (gGeNaIb.inGate(ecGe, ecNaI) && gTAC_Coin1.inGate(eTAC_Coin)) {
			hGe_2.inc(eGe);
		}
		if (gGeNaIc.inGate(ecGe, ecNaI) && gTAC_Coin1.inGate(eTAC_Coin)) {
			hGe_3.inc(eGe);
		}
		if (gGeNaIa.inGate(ecGe, ecNaI) && gTAC_Coin2.inGate(eTAC_Coin)) {
			hGe_4.inc(eGe);
		}
		if (gGeNaIb.inGate(ecGe, ecNaI) && gTAC_Coin2.inGate(eTAC_Coin)) {
			hGe_5.inc(eGe);
		}
		if (gGeNaIc.inGate(ecGe, ecNaI) && gTAC_Coin2.inGate(eTAC_Coin)) {
			hGe_6.inc(eGe);
		}
		// **** NaI Threshold Conditions **************************

		if (gAC_short_thresh.inGate(eNaI) && gTAC_Anti1.inGate(eTAC_Anti)) {
			hAC_short_thresh.inc(eGe);
		}
		if (gAC_short_thresh.inGate(eNaI) && !gTAC_Anti1.inGate(eTAC_Anti)) {
			hAC_short_thresh.inc(eGe);
		}
		if (!gAC_short_thresh.inGate(eNaI) && !gTAC_Anti1.inGate(eTAC_Anti)) {
			hAC_short_thresh.inc(eGe);
		}
		if (gAC_long_thresh.inGate(eNaI) && gTAC_Anti2.inGate(eTAC_Anti)) {
			hAC_long_thresh.inc(eGe);
		}
		if (gAC_long_thresh.inGate(eNaI) && !gTAC_Anti1.inGate(eTAC_Anti)) {
			hAC_long_thresh.inc(eGe);
		}
		if (!gAC_long_thresh.inGate(eNaI) && !gTAC_Anti1.inGate(eTAC_Anti)) {
			hAC_long_thresh.inc(eGe);
		}

	}

	/**
	 * monitor method
	 * calculate the live time
	 */
	public double monitor(String name) {
		return 0.0;
	}

}
