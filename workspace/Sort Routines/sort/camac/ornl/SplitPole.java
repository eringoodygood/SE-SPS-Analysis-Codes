/* 
*/
package sort.camac.ornl;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for Enge SplitPole Online
 * Has a singles Silicon monitor detector
 *
 *  convention for 2 d Histograms x first then y (x vs y)
 *
 * Author Ken Swartz, Alan Chen, and Dale Visser
 * last modified June 99 by Rachel Lewis
 */
public class SplitPole extends SortRoutine {

	// ungated spectra 
	Histogram hCthd;
	Histogram hAnde;
	Histogram hSntr1;
	Histogram hSntr2;
	Histogram hSntrSum;
	Histogram hFrntPsn;
	Histogram hRearPsn;
	Histogram hFrntHgh; //front Wire Pulse Height
	Histogram hRearHgh; //Rear Wire Pulse Height
	Histogram hFrntPH; // position x height y
	Histogram hRearPH;
	Histogram hCthdAnde;
	Histogram hSntrCthd;
	Histogram hFrntCthd;
	Histogram hFrntAnde;
	Histogram hFrntSntr;
	Histogram hFrntPRearP;

	Histogram hSilicon;

	//gate by scintillator cathode
	Histogram hFrntSntrGSC;
	Histogram hFrntCthdGSC;

	//gate by Front wire Cathode
	Histogram hSntrCthdGFC;
	Histogram hFrntSntrGFC;

	//gate by Front wire Scintillator
	Histogram hSntrCthdGFS;
	Histogram hFrntCthdGFS;

	//front and rear wire gate on all    
	Histogram hFrntGCSF;
	Histogram hRearGCSF;
	Histogram hFrntGAll;
	Histogram hRearGAll;

	//front and rear wire gated on All compressed
	Histogram hcFrntGAll;
	Histogram hcRearGAll;

	// ungated spectra 
	Histogram hGe;
	Histogram hCoinTAC;
	Histogram hGeGate;

	//gates 1 d
	Gate gSilicon;
	Gate gCthd;
	Gate gPeak;
	Gate gGood;

	//gates 2 d		 
	Gate gSntrCthd;
	Gate gFrntSntr;
	Gate gFrntCthd;
	Gate gFrntRear;

	//scalers
	Scaler sBic;
	Scaler sClck;
	Scaler sEvntRaw;
	Scaler sEvntAccpt;
	Scaler sSilRaw;
	Scaler sSilAccpt;
	Scaler sCathode;
	Scaler sScintillator;
	Scaler sFront;

	//monitors
	Monitor mBeam;
	Monitor mClck;
	Monitor mEvntRt;
	Monitor mCthd;
	Monitor mGood;
	Monitor mLive;

	//id numbers for the signals;
	int idMonitor;
	int idCthd;
	int idAnde;
	int idScintR;
	int idScintL;
	int idFrntPsn;
	int idRearPsn;
	int idFrntHgh; //front Wire Pulse Height
	int idRearHgh; //Rear Wire Pulse Height
	int idFrntPH; // position x height y
	int idRearPH;
	int lastEvntAccpt;

	public void initialize() throws Exception {

		//EVENT_SIZE=9;    	
		//	***check the addresses for these events***
		//               <C, N, A, F>
		cnafCommands.init(1, 28, 8, 26); //crate dataway Z 	
		cnafCommands.init(1, 28, 9, 26); //crate dataway C
		cnafCommands.init(1, 30, 9, 26); //crate I
		cnafCommands.init(1, 3, 12, 11); //adc 811 clear
		cnafCommands.init(1, 9, 12, 11); //adc 811 clear
		cnafCommands.init(1, 20, 0, 10); //trigger module clear

		//event return id number to be used in sort 
		idCthd = cnafCommands.eventRead(1, 3, 0, 0);
		//read first channel:Cathode signal
		idAnde = cnafCommands.eventRead(1, 3, 1, 0); //read Anode
		idScintR = cnafCommands.eventRead(1, 3, 2, 0);
		//read first channel:Cathode signal
		idScintL = cnafCommands.eventRead(1, 3, 3, 0); //read Anode
		idFrntPsn = cnafCommands.eventRead(1, 3, 4, 0); //read Front TAC
		idRearPsn = cnafCommands.eventRead(1, 3, 5, 0); //read Rear TAC
		idFrntHgh = cnafCommands.eventRead(1, 3, 6, 0);
		//read front wire pulse height
		idRearHgh = cnafCommands.eventRead(1, 3, 7, 0);
		//read rear wire pulse height
		idMonitor = cnafCommands.eventRead(1, 9, 1, 0);
		//read beam monitor	    (slot 9 channel 1)

		cnafCommands.eventCommand(1, 3, 12, 11); //clear adc
		cnafCommands.eventCommand(1, 9, 12, 11); //clear adc
		cnafCommands.eventCommand(1, 20, 0, 10); //clear trigger module

		cnafCommands.scaler(1, 16, 0, 0); //read scaler BIC
		cnafCommands.scaler(1, 16, 1, 0); //read scaler Clock
		cnafCommands.scaler(1, 16, 2, 0); //read scaler Event Raw
		cnafCommands.scaler(1, 16, 3, 0); //read scaler Event Accept
		cnafCommands.scaler(1, 16, 4, 0); //read # Cathode strobes
		cnafCommands.scaler(1, 16, 5, 0); //read # Scint strobes
		cnafCommands.scaler(1, 16, 6, 0); //read # FW strobes

		cnafCommands.clear(1, 16, 0, 9); //clear scaler

		hSilicon =
			new Histogram("Silicon     ", HIST_1D_INT, 2048, "Beam Monitor");
		hCthd =
			new Histogram("Cathode     ", HIST_1D_INT, 2048, "Cathode Raw ");
		hAnde = new Histogram("Anode       ", HIST_1D_INT, 2048, "Anode Raw");
		hSntr1 =
			new Histogram(
				"Scint1      ",
				HIST_1D_INT,
				2048,
				"Scintillator PMT 1");
		hSntr2 =
			new Histogram(
				"Scint2      ",
				HIST_1D_INT,
				2048,
				"Scintillator PMT 2");
		hSntrSum =
			new Histogram(
				"ScintSum    ",
				HIST_1D_INT,
				2048,
				"Scintillator Sum");
		hFrntPsn =
			new Histogram(
				"FrontPosn    ",
				HIST_1D_INT,
				2048,
				"Front Wire Position");
		hRearPsn =
			new Histogram(
				"RearPosn     ",
				HIST_1D_INT,
				2048,
				"Rear Wire Position");
		hFrntHgh =
			new Histogram(
				"FrontHeight   ",
				HIST_1D_INT,
				2048,
				"Front Wire Pulse Height");
		hRearHgh =
			new Histogram(
				"RearHeight    ",
				HIST_1D_INT,
				2048,
				"Rear Wire Pulse Height");
		hFrntPH =
			new Histogram(
				"FrontPvsHeight",
				HIST_2D_INT,
				256,
				"Pulse Height vs Front Position",
				"Front Position",
				"Pulse Height");
		hRearPH =
			new Histogram(
				"RearPvsHeight ",
				HIST_2D_INT,
				256,
				"Pulse Height vs Rear Position",
				"Rear Position",
				"Pulse Height");
		hCthdAnde =
			new Histogram(
				"CathodeAnode  ",
				HIST_2D_INT,
				256,
				"Cathode vs Anode ",
				"Cathode",
				"Anode");
		hSntrCthd =
			new Histogram(
				"ScintCathode  ",
				HIST_2D_INT,
				256,
				"Cathode vs Scintillator",
				"Scintillator",
				"Cathode");
		hFrntCthd =
			new Histogram(
				"FrontCathode  ",
				HIST_2D_INT,
				256,
				"Cathode vs Front Position",
				"Front Position",
				"Cathode");
		hFrntAnde =
			new Histogram(
				"FrontAnode  ",
				HIST_2D_INT,
				256,
				"Anode vs Front Position",
				"Front Position",
				"Anode");
		hFrntSntr =
			new Histogram(
				"FrontScint ",
				HIST_2D_INT,
				256,
				"Scintillator vs Front Position",
				"Front Position",
				"Scintillator");
		hFrntPRearP =
			new Histogram(
				"FrontRear  ",
				HIST_2D_INT,
				256,
				"Rear Position vs Front Position",
				"Front Position",
				"Rear Position");

		//gate on Scintillator Cathode
		hFrntSntrGSC =
			new Histogram(
				"FrontScintGSC ",
				HIST_2D_INT,
				256,
				"Scintillator vs Front Position - ScCa gate",
				"Front Position",
				"Scintillator");
		hFrntCthdGSC =
			new Histogram(
				"FrontCathodeGSC",
				HIST_2D_INT,
				256,
				"Cathode vs Front Position - ScCa gate",
				"Front Position",
				"Cathode");

		//gate on Front Wire Cathode
		hSntrCthdGFC =
			new Histogram(
				"ScintCathodeGFC",
				HIST_2D_INT,
				256,
				"Cathode vs Scintillator - FwCa gate",
				"Scintillator",
				"Cathode");
		hFrntSntrGFC =
			new Histogram(
				"FrontScintGFC",
				HIST_2D_INT,
				256,
				"Scintillator vs Front Position - FwCa gate",
				"Front Position",
				"Scintillator");

		//gate on Front Wire Scintillator
		hSntrCthdGFS =
			new Histogram(
				"ScintCathodeGFS",
				HIST_2D_INT,
				256,
				"Cathode vs Scintillator - FwSc gate",
				"Scintillator",
				"Cathode");
		hFrntCthdGFS =
			new Histogram(
				"FrontCathodeGFS ",
				HIST_2D_INT,
				256,
				"Cathode vs Front Position - FwSc gate ",
				"Front Position",
				"Cathode");

		//gate on 3 gate 
		hFrntGCSF =
			new Histogram(
				"FrontGCSF    ",
				HIST_1D_INT,
				2048,
				"Front Position - ScCa,FwCa,FwSc gates");
		hRearGCSF =
			new Histogram(
				"RearGCSF    ",
				HIST_1D_INT,
				2048,
				"Rear Position - ScCa,FwCa,FwSc gates");

		//gate on 3 gate 
		hFrntGAll =
			new Histogram(
				"FrontGAll    ",
				HIST_1D_INT,
				2048,
				"Front Position - ScCa,FwCa,FwSc,FwRw gates");
		hRearGAll =
			new Histogram(
				"RearGAll    ",
				HIST_1D_INT,
				2048,
				"Rear Position - ScCa,FwCa,FwSc,FwRw gates");
		hcFrntGAll =
			new Histogram(
				"FrontGAllcmp ",
				HIST_1D_INT,
				1024,
				"Front Position comp - ScCa,FwCa,FwSc,FwRw gates");
		hcRearGAll =
			new Histogram(
				"RearGAllcmp ",
				HIST_1D_INT,
				1024,
				"Rear Position comp - ScCa,FwCa,FwSc,FwRw gates");

		// gates 1d
		gCthd = new Gate("Counts", hCthd);
		gPeak = new Gate("Peak", hFrntGCSF);
		gSilicon = new Gate("Elastics", hSilicon);
		gGood = new Gate("GoodEvent", hFrntGAll);

		//gates  2d
		gSntrCthd = new Gate("Ca-Sc", hSntrCthd);
		//gate on Scintillator Cathode
		gFrntSntr = new Gate("Fw-Sc", hFrntSntr); //gate on Front Scintillator
		gFrntCthd = new Gate("Fw-Ca", hFrntCthd); //gate on Front Cathode
		gFrntRear = new Gate("Fw-Rw", hFrntPRearP); //gate on Front Rear	
		hFrntSntrGSC.addGate(gFrntSntr);
		hFrntCthdGSC.addGate(gFrntCthd);
		hSntrCthdGFC.addGate(gSntrCthd);
		hFrntSntrGFC.addGate(gFrntSntr);
		hSntrCthdGFS.addGate(gSntrCthd);
		hFrntCthdGFS.addGate(gFrntCthd);

		//scalers
		sBic = new Scaler("BIC", 0);
		sClck = new Scaler("Clock", 1);
		sEvntRaw = new Scaler("Event Raw", 2);
		sEvntAccpt = new Scaler("Event Accept", 3);
		sCathode = new Scaler("Cathode", 4);
		sScintillator = new Scaler("Scintillator", 5);
		sFront = new Scaler("Front Wire", 6);

		//monitors
		mBeam = new Monitor("Beam ", sBic);
		mClck = new Monitor("Clock", sClck);
		mEvntRt = new Monitor("Event Rate", sEvntRaw);
		mCthd = new Monitor("Cathode", gCthd);
		mGood = new Monitor("Good Events", gGood);

		mLive = new Monitor("Live Time", this);
	}
	public void sort(int[] dataEvent) throws Exception {

		//unpack data into convenient names
		int eCthd = dataEvent[idCthd];
		int eAnde = dataEvent[idAnde];
		int eSntr1 = dataEvent[idScintR];
		int eSntr2 = dataEvent[idScintL];
		int eFPsn = dataEvent[idFrntPsn];
		int eRPsn = dataEvent[idRearPsn];
		int eFHgh = dataEvent[idFrntHgh];
		int eRHgh = dataEvent[idRearHgh];
		int eSil = dataEvent[idMonitor];

		int eSntr = (eSntr1 + eSntr2) / 2;

		int ecFPsn = eFPsn >> 3;
		int ecRPsn = eRPsn >> 3;
		int ecFHgh = eFHgh >> 3;
		int ecRHgh = eRHgh >> 3;

		int ecSntr = eSntr >> 3;
		int ecCthd = eCthd >> 3;
		int ecAnde = eAnde >> 3;

		// singles spectra    
		hCthd.inc(eCthd);
		hAnde.inc(eAnde);
		hSntr1.inc(eSntr1);
		hSntr2.inc(eSntr2);
		hSntrSum.inc(eSntr);
		hFrntPsn.inc(eFPsn);
		hRearPsn.inc(eRPsn);
		hFrntHgh.inc(eFHgh);
		hRearHgh.inc(eRHgh);
		hSilicon.inc(eSil);

		//singles 2d spectra	
		hFrntPH.inc(ecFPsn, ecFHgh);
		hRearPH.inc(ecRPsn, ecRHgh);
		hCthdAnde.inc(ecCthd, ecAnde);
		hSntrCthd.inc(ecSntr, ecCthd);
		hFrntCthd.inc(ecFPsn, ecCthd);
		hFrntAnde.inc(ecFPsn, ecAnde);
		hFrntSntr.inc(ecFPsn, ecSntr);
		hFrntPRearP.inc(ecFPsn, ecRPsn);

		// gate on Scintillator vs Cathode
		if (gSntrCthd.inGate(ecSntr, ecCthd)) {
			hFrntSntrGSC.inc(ecFPsn, ecSntr);
			hFrntCthdGSC.inc(ecFPsn, ecCthd);

		}
		// gate on Front Wire Position vs Cathode	
		if (gFrntCthd.inGate(ecFPsn, ecCthd)) {
			hSntrCthdGFC.inc(ecSntr, ecCthd);
			hFrntSntrGFC.inc(ecFPsn, ecSntr);

		}
		// gate on Front Wire Position vs Scintillator	
		if (gFrntSntr.inGate(ecFPsn, ecSntr)) {
			hSntrCthdGFS.inc(ecSntr, ecCthd);
			hFrntCthdGFS.inc(ecFPsn, ecCthd);

		}
		// gated on all 3 gate above
		if ((gSntrCthd.inGate(ecSntr, ecCthd))
			&& (gFrntCthd.inGate(ecFPsn, ecCthd))
			&& (gFrntSntr.inGate(ecFPsn, ecSntr))) {
			hFrntGCSF.inc(eFPsn);
			hRearGCSF.inc(eRPsn);
		}

		// gate on all 3 gates above and the Front wire vs Rear Wire	
		if ((gSntrCthd.inGate(ecSntr, ecCthd))
			&& (gFrntCthd.inGate(ecFPsn, ecCthd))
			&& (gFrntSntr.inGate(ecFPsn, ecSntr))
			&& (gFrntRear.inGate(ecFPsn, ecRPsn))) {
			hFrntGAll.inc(eFPsn);
			hRearGAll.inc(eRPsn);
			hcFrntGAll.inc(eFPsn >> 2);
			hcRearGAll.inc(eRPsn >> 2);
		}

	}
	/**
	 * monitor method
	 * calculate the live time
	 */
	public double monitor(String name) {
		int rateEvntAccpt = sEvntAccpt.getValue() - lastEvntAccpt;
		lastEvntAccpt = sEvntAccpt.getValue();

		if (name.equals("Live Time")) {
			if (mEvntRt.getValue() > 0) {
				return 100.0 * rateEvntAccpt / mEvntRt.getValue();
			} else {
				return 0.0;
			}
		} else {
			return 50.0;
		}
	}
}
