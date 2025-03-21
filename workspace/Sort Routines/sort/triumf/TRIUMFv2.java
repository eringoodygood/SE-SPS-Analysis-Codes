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
 */
public class TRIUMFv2 extends SortRoutine {


  // ungated spectra
  static final int NUM_ANODES=5;
  Histogram [] Anode = new Histogram [NUM_ANODES];
  Histogram FirstFour;
  Histogram TotalEnergy;
  static final int NUM_SILICON=2;
  Histogram [] Silicon=new Histogram[NUM_SILICON];

  //2D PID views
  Histogram h1vsTot;
  Histogram h12vsTot;
  Histogram h123vsTot;
  Histogram h1234vsTot;

  Histogram h1vs2345;
  Histogram h12vs2345;

  Histogram h12vs345;
  Histogram h123vs345;

  Histogram h123vs45;

  Histogram h1vs1234;
  Histogram h12vs1234;
  Histogram h123vs1234;

  Histogram h1vs234;
  Histogram h12vs234;
  Histogram h12vs34;
  Histogram h123vs4;

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
      Anode[i] = new Histogram("Anode "+(i+1), HIST_1D_INT, 2048, "Anode "+(i+1),"Energy",
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
    
    TotalEnergy = new Histogram("Total Energy", HIST_1D_INT, 5*2048,
                  "Total Energy in All Anodes","Energy", "Counts");

    Silicon[0] = new Histogram("Si Ultra", HIST_1D_INT, 2048, "Silicon Ultra Energy",
                    "Energy","Counts");
    Silicon[1] = new Histogram("Si Elastic", HIST_1D_INT, 2048, "Silicon Elastoc Energy",
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

    h1vsTot = new Histogram("1 vs Total", HIST_2D_INT,  256, "Anode 1 vs. Total",
    "Total","Anode 1");
    h12vsTot = new Histogram("12 vs Total", HIST_2D_INT,  256, "Anodes 1+2 vs. Total",
    "Total","Anodes 1+2");
    h123vsTot = new Histogram("123 vs Total", HIST_2D_INT,  256, "Anodes 1+2+3 vs. Total",
    "Total","Anodes 1+2+3");
    h1234vsTot = new Histogram("1234 vs Total", HIST_2D_INT,  256, "Anodes 1+2+3+4 vs. Total",
    "Total","Anodes 1+2+3+4");

    h1vs2345 = new Histogram("1 vs 2345", HIST_2D_INT,  256, "Anode 1 vs. Anodes 2+3+4+5",
    "Anodes 2+3+4+5","Anode 1");
    h12vs2345 = new Histogram("12 vs 2345", HIST_2D_INT,  256, "Anodes 1+2 vs. Anodes 2+3+4+5",
    "Anodes 2+3+4+5","Anodes 1+2");

    h12vs345 = new Histogram("12 vs 345", HIST_2D_INT,  256, "Anodes 1+2 vs. Anodes 3+4+5",
    "Anodes 3+4+5","Anodes 1+2");
    h123vs345 = new Histogram("123 vs 345", HIST_2D_INT,  256, "Anode2 1+2+3 vs. Anodes 3+4+5",
    "Anodes 3+4+5","Anodes 1+2+3");

    h123vs45 = new Histogram("123 vs 45", HIST_2D_INT,  256, "Anodes 1+2+3 vs. Anodes 4+5",
    "Anodes 4+5","Anodes 1+2+3");

    FirstFour = new Histogram("First Four", HIST_1D_INT, 4*2048,
    "Total Energy in First 4 Anodes","Energy", "Counts");

    h1vs1234 = new Histogram("1 vs 1234", HIST_2D_INT,  256, "Anode 1 vs. Anodes 1+2+3+4",
    "Anodes 1+2+3+4","Anode 1");
    h12vs1234 = new Histogram("12 vs 1234", HIST_2D_INT,  256, "Anodes 1+2 vs. Anodes 1+2+3+4",
    "Anodes 1+2+3+4","Anodes 1+2");
    h123vs1234 = new Histogram("123 vs 1234", HIST_2D_INT,  256, "Anodes 1+2+3 vs. Anodes 1+2+3+4",
    "Anodes 1+2+3+4","Anodes 1+2+3");

    h1vs234 = new Histogram("1 vs 234", HIST_2D_INT,  256, "Anode 1 vs. Anodes 2+3+4",
    "Anodes 2+3+4","Anode 1");
    h12vs234 = new Histogram("12 vs 234", HIST_2D_INT,  256, "Anodes 1+2 vs. Anodes 2+3+4",
    "Anodes 2+3+4","Anodes 1+2");
    h12vs34 = new Histogram("12 vs 34", HIST_2D_INT,  256, "Anodes 1+2 vs. Anodes 3+4",
    "Anodes 3+4","Anodes 1+2");
    h123vs4 = new Histogram("123 vs 4", HIST_2D_INT,  256, "Anodes 1+2+3 vs. Anode 4",
    "Anode 4","Anodes 1+2+3");



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
    int ec1 = eAnode[0] >> 2;
    int ec12 = (eAnode[0]+eAnode[1]) >>2;
    int ec123 = (eAnode[0]+eAnode[1]+eAnode[2]) >>2;
    int e1234 = eAnode[0]+eAnode[1]+eAnode[2]+eAnode[3];
    int ec1234 = e1234 >> 2;
    int eTot = eAnode[0]+eAnode[1]+eAnode[2]+eAnode[3]+eAnode[4];
    int ecTot = eTot >> 2 ;
    int ec2345 = (eAnode[1]+eAnode[2]+eAnode[3]+eAnode[4]) >> 2;
    int ec345 = (eAnode[2]+eAnode[3]+eAnode[4]) >> 2;
    int ec45 = (eAnode[3]+eAnode[4]) >> 2;

    int ec234 = (eAnode[1]+eAnode[2]+eAnode[3]) >> 2;
    int ec34 = (eAnode[2]+eAnode[3]) >> 2;

    int ec4 = eAnode[3]>>2;

    TotalEnergy.inc(eTot);

    // 2D spectra
    h1vsTot.inc(ecTot,ec1);
    h12vsTot.inc(ecTot,ec12);
    h123vsTot.inc(ecTot,ec123);
    h1234vsTot.inc(ecTot,ec1234);

    h1vs2345.inc(ec2345,ec1);
    h12vs2345.inc(ec2345,ec12);

    h12vs345.inc(ec345,ec12);
    h123vs345.inc(ec345,ec123);

    h123vs45.inc(ec45,ec123);

    FirstFour.inc(e1234);

    h1vs1234.inc(ec1234,ec1);
    h12vs1234.inc(ec1234,ec12);
    h123vs1234.inc(ec1234,ec123);

    h1vs234.inc(ec234,ec1);
    h12vs234.inc(ec234,ec12);
    h12vs34.inc(ec34,ec12);
    h123vs4.inc(ec4,ec123);
  }
}
