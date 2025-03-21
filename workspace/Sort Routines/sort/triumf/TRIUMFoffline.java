/*
 */
package sort.triumf;
import jam.data.*;
import jam.sort.SortRoutine;
/*
 * Sort file for DRAGON ion counter and silicon tests.
 *
 * @author Dale Visser
 * @author Alan Chen
 * @author Rachel Lewis (modifications for offline sorting)
 * increment 2d histograms only if there is an event in each anode
 */
public class TRIUMFoffline extends SortRoutine {


  // ungated spectra
  static final int NUM_ANODES=5;
  Histogram [] Anode = new Histogram [NUM_ANODES];
  Histogram FirstFour;
  Histogram TotalEnergy;
  static final int NUM_SILICON=2;
  Histogram [] Silicon=new Histogram[NUM_SILICON];

  //2D PID views
  Histogram h1vsTotAll;
  Histogram h1vsTot;
  Histogram h2vsTot;
  Histogram h3vsTot;
  Histogram h4vsTot;
  Histogram h5vsTot;
  Histogram h2vs1345;
  Histogram h3vs1245;
  Histogram h4vs1235;
  Histogram h5vs1234;
  Histogram h1vs2345;
  Histogram h1vs1234;

  //gated on anode 1 vs total
  Histogram h2vsTotG;
  Histogram h3vsTotG;
  Histogram h4vsTotG;
  Histogram h5vsTotG;

  //gate in 1vstot
  Gate g1vsTot;
  //scalers
  Scaler BCI;
  Scaler Clock;
  Scaler EventTrigger;
  Scaler EventAccept;

  //monitors
  Monitor mBCI;
  Monitor mClock;
  Monitor mEventTrigger;
  Monitor mEventAccept;

  //id numbers for the signals;
  int [] idAnode = new int[NUM_ANODES];
  int [] idSilicon = new int[NUM_SILICON];

  //event data
  int [] eAnode=new int[NUM_ANODES];
  int [] eSilicon = new int [NUM_SILICON];

  //sort parameters
  //value in sort = (ADC value - offsete)*gain
  DataParameter [] AnodeOffset=new DataParameter[NUM_ANODES];
  DataParameter [] AnodeGain=new DataParameter[NUM_ANODES];

  public void initialize() throws Exception {


    //  ***check the addresses for these events***
    //               <C, N, A, F>
    cnafCommands.init(1,28,8,26);      //crate dataway Z
    cnafCommands.init(1,28,9,26);      //crate dataway C
    cnafCommands.init(1,30,9,26);      //crate I
    cnafCommands.init(1,3,12,11);      //adc 811 clear
    cnafCommands.init(1,20,0,10);      //trigger module clear

    //event return id number to be used in sort
    for (int i=0;i<NUM_ANODES;i++) {
      Anode[i] = new Histogram("Anode "+(i+1), HIST_1D_INT, 512, "Anode "+(i+1),"Energy",
      "Counts");
      AnodeOffset[i]=new DataParameter("Offset "+(i+1));
      AnodeOffset[i].setValue(0.0);
      AnodeGain[i]=new DataParameter("Gain "+(i+1));
      AnodeGain[i].setValue(1.0);
    }
    idAnode[0]=cnafCommands.eventRead(1,3,0,0);//Anode 1
    idAnode[1]=cnafCommands.eventRead(1,3,1,0);//Anode 2
    idAnode[2]=cnafCommands.eventRead(1,3,5,0);//Anode 3
    idAnode[3]=cnafCommands.eventRead(1,3,3,0);//Anode 4
    idAnode[4]=cnafCommands.eventRead(1,3,4,0);//Anode 5

    TotalEnergy = new Histogram("Total Energy", HIST_1D_INT, 5*512,
    "Total Energy in All Anodes","Energy", "Counts");
    FirstFour = new Histogram("First Four", HIST_1D_INT, 4*512,
    "Total Energy in First 4 Anodes","Energy", "Counts");

    Silicon[0] = new Histogram("Si Ultra", HIST_1D_INT, 512, "Silicon Ultra Energy",
    "Energy","Counts");
    Silicon[1] = new Histogram("Si Elastic", HIST_1D_INT, 512, "Silicon Elastic Energy",
    "Energy","Counts");

    idSilicon[0] = cnafCommands.eventRead(1,3,6,0); //Silicon Ultra
    idSilicon[1] = cnafCommands.eventRead(1,3,7,0); //Silicon Elastic


    cnafCommands.eventCommand(1,3,12,11);        //clear Ortec 811 ADC in slot 3, crate 1
    cnafCommands.eventCommand(1,20,0,10);        //clear trigger module in slot 20, crate 1

    cnafCommands.scaler(1,16,0,0);        //read scaler BCI
    cnafCommands.scaler(1,16,1,0);        //read scaler Clock
    cnafCommands.scaler(1,16,2,0);        //read scaler Trigger Raw
    cnafCommands.scaler(1,16,3,0);        //read scaler Trigger Accept

    cnafCommands.clear(1,16,0,9);        //clear scaler

    h1vsTot = new Histogram("1 vs Total", HIST_2D_INT,  512, "Anode 1 vs. Total",
    "Total","Anode 1");
    h1vsTotAll = new Histogram("1 vs Tot (all)", HIST_2D_INT,  512, "Anode 1 vs. Total--including particles that stop before anode 5",
    "Total","Anode 1");
    h1vs1234 = new Histogram("1 vs 1234", HIST_2D_INT,  512, "Anode 1 vs. Anodes 1+2+3+4",
    "Anodes 1+2+3+4","Anode 1");
    h2vsTot = new Histogram("2 vs Total", HIST_2D_INT,  512, "Anode 2 vs. Total",
    "Total","Anode 2");
    h3vsTot = new Histogram("3 vs Total", HIST_2D_INT,  512, "Anode 3 vs. Total",
    "Total","Anode 3");
    h4vsTot = new Histogram("4 vs Total", HIST_2D_INT,  512, "Anode 4 vs. Total",
    "Total","Anode 4");
    h5vsTot = new Histogram("5 vs Total", HIST_2D_INT,  512, "Anode 5 vs. Total",
    "Total","Anode 5");

    h1vs2345 = new Histogram("1 vs 2345", HIST_2D_INT,  512, "Anode 1 vs. 2345",
    "Anodes 2+3+4+5","Anode 1");
    h2vs1345 = new Histogram("2 vs 1345", HIST_2D_INT,  512, "Anode 2 vs. 1345",
    "1345","Anode 2");
    h3vs1245 = new Histogram("3 vs 1245", HIST_2D_INT,  512, "Anode 3 vs. 1245",
    "1245","Anode 3");
    h4vs1235 = new Histogram("4 vs 1235", HIST_2D_INT,  512, "Anode 4 vs. 1235",
    "1235","Anode 4");
    h5vs1234 = new Histogram("5 vs 1234", HIST_2D_INT,  512, "Anode 5 vs. 1234",
    "1234","Anode 5");
    h2vsTotG = new Histogram("2 vs Total Gated on 1 vs Total", HIST_2D_INT,  512, "Anode 2 vs. Total",
    "Total","Anode 2");
    h3vsTotG = new Histogram("3 vs Total Gated on 1 vs Total", HIST_2D_INT,  512, "Anode 3 vs. Total",
    "Total","Anode 3");
    h4vsTotG = new Histogram("4 vs Total Gated on 1 vs Total", HIST_2D_INT,  512, "Anode 4 vs. Total",
    "Total","Anode 4");
    h5vsTotG = new Histogram("5 vs Total Gated on 1 vs Total", HIST_2D_INT,  512, "Anode 5 vs. Total",
    "Total","Anode 5");

    //gate
    g1vsTot = new Gate("Gate 1", h1vsTot);

    //scalers
    BCI      =new Scaler("BCI",0);
    Clock      =new Scaler("Clock",1);
    EventTrigger    =new Scaler("Event Trigger", 2);
    EventAccept  =new Scaler("Event Accept",3);

    //monitors
    mBCI=new Monitor("BCI",BCI);
    mClock=new Monitor("Clock",Clock);
    mEventTrigger=new Monitor("Event Triggers",EventTrigger);
    mEventAccept=new Monitor("Accepted Events",EventAccept);
  }

  public void sort(int [] data) throws Exception {
    //unpack data into convenient names
    for (int i=0;i<NUM_ANODES;i++) {
      eAnode[i] = (int)( (data[idAnode[i]]-AnodeOffset[i].getValue())
      * AnodeGain[i].getValue());
      Anode[i].inc(eAnode[i]);
    }
    for (int i=0;i<NUM_SILICON;i++) {
      eSilicon[i] = data[idSilicon[i]];
      Silicon[i].inc(eSilicon[i]);
    }

    //compressed combinations for 2D spectra
    int ec1 = eAnode[0] >> 1;
    int ec2 = eAnode[1] >> 1;
    int ec3 = eAnode[2] >> 1;
    int ec4 = eAnode[3] >> 1;
    int ec5 = eAnode[4] >> 1;
    int e1234 = eAnode[0]+eAnode[1]+eAnode[2]+eAnode[3];
    int e1235 = eAnode[0]+eAnode[1]+eAnode[2]+eAnode[4];
    int e1245 = eAnode[4]+eAnode[1]+eAnode[3]+eAnode[0];
    int e1345 = eAnode[4]+eAnode[2]+eAnode[0]+eAnode[3];
    int e2345 = eAnode[1]+eAnode[2]+eAnode[3]+eAnode[4];
    int ec1234 = e1234 >> 1;
    int ec1235 = e1235 >> 1;
    int ec1245 = e1245 >> 1;
    int ec1345 = e1345 >> 1;
    int ec2345 = e2345 >> 1;
    int eTot = eAnode[0]+eAnode[1]+eAnode[2]+eAnode[3]+eAnode[4];
    int ecTot = eTot >> 1 ;

    TotalEnergy.inc(eTot);
    FirstFour.inc(e1234);
    h1vsTotAll.inc(ecTot,eAnode[0]);
    //apply event condition
    if ( eAnode[0]>5 && eAnode[1]>5 && eAnode[2]>5 && eAnode[3]>5 && eAnode[4]>5){
      writeEvent(data);//this is when it writes presorted file if requested at sort time

      // 2D spectra
      h1vsTot.inc(ecTot,eAnode[0]);
      h2vsTot.inc(ecTot,eAnode[1]);
      h3vsTot.inc(ecTot,eAnode[2]);
      h4vsTot.inc(ecTot,eAnode[3]);
      h5vsTot.inc(ecTot,eAnode[4]);

      h1vs2345.inc(ec2345,eAnode[0]);
      h2vs1345.inc(ec1345,eAnode[1]);
      h3vs1245.inc(ec1245,eAnode[2]);
      h4vs1235.inc(ec1235,eAnode[3]);
      h5vs1234.inc(ec1234,eAnode[4]);

      h1vs1234.inc(ec1234,eAnode[0]);
      //write gated spectra
      if ( g1vsTot.inGate(eTot,eAnode[0]) ){
        h2vsTotG.inc(ecTot,eAnode[1]);
        h3vsTotG.inc(ecTot,eAnode[2]);
        h4vsTotG.inc(ecTot,eAnode[3]);
        h5vsTotG.inc(ecTot,eAnode[4]);
      }//end gating
    }//end event condition
  }
}
