#include "preprocess.h"
#include "eventbuild.h"
#include <TCutG.h>
#include <TROOT.h>
#include <TStyle.h>
#include <iostream>
#include <TLegend.h>
#include <TFile.h>
#include <TH1F.h>
//#include "FocalPlane_SABRE.h"
//#include "progressbar2.h"
#include <TH2F.h>
#include <TTree.h>
#include <TCanvas.h>
//#include "TTreeReader.h"
//#include "TTreeReaderValue.h"

void processOneSegment_Nov2019_withcuts(int runID, int segID, bool skip_evb)
{
/*  Anything but the rainbow colormap
kDeepSea=51,          kGreyScale=52,    kDarkBodyRadiator=53,
kBlueYellow= 54,                        kInvertedDarkBodyRadiator=56,
kBird=57,             kCubehelix=58,    kGreenRedViolet=59,
kBlueRedYellow=60,    kOcean=61,        kColorPrintableOnGrey=62,
kAlpine=63,           kAquamarine=64,   kArmy=65,
kAtlantic=66,         kAurora=67,       kAvocado=68,
kBeach=69,            kBlackBody=70,    kBlueGreenYellow=71,
kBrownCyan=72,        kCMYK=73,         kCandy=74,
kCherry=75,           kCoffee=76,       kDarkRainBow=77,
kDarkTerrain=78,      kFall=79,         kFruitPunch=80,
kFuchsia=81,          kGreyYellow=82,   kGreenBrownTerrain=83,
kGreenPink=84,        kIsland=85,       kLake=86,
kLightTemperature=87, kLightTerrain=88, kMint=89,
kNeon=90,             kPastel=91,       kPearl=92,
kPigeon=93,           kPlum=94,         kRedBlue=95,
kRose=96,             kRust=97,         kSandyTerrain=98,
kSienna=99,           kSolar=100,       kSouthWest=101,
kStarryNight=102,     kSunset=103,      kTemperatureMap=104,
kThermometer=105,     kValentine=106,   kVisibleSpectrum=107,
kWaterMelon=108,      kCool=109,        kCopper=110,
kGistEarth=111,       kViridis=112,     kCividis=113*/

//int runNumber_start = 319;
//int runNumber_end = 320;
//int maxSegments = 0;

	int i = runID;
    int j = segID;
//	int i = runNumber;
//	int j = segNumber;

	gStyle->SetOptStat("nei");
	gStyle->SetPalette(kInvertedDarkBodyRadiator);//kDarkBodyRadiator   kInvertedDarkBodyRadiator
	gROOT->ProcessLine("gErrorIgnoreLevel = 1001;"); //magically turns off annoying ROOT messages.

	//TFile *fprotoncut = new TFile("protons.root");
	//TCutG *protons = static_cast<TCutG*>(fprotoncut->Get("protons"));
	//protons->SetName("protons");

	
	//Erin's cuts
	TFile *tightgate = new TFile("cuts_erin/tightgate.root");
	TCutG *tighttriton = static_cast<TCutG*>(tightgate->Get("tighttriton"));
	tighttriton->SetName("tighttriton");
	
	TFile *deuteronsfile = new TFile("cuts_erin/deuterons.root");
	TCutG *deuterons = static_cast<TCutG*>(deuteronsfile->Get("deuterons"));
	deuterons->SetName("deuterons");
	
	TFile *alphafile = new TFile("cuts_erin/alphas.root");
	TCutG *alphas = static_cast<TCutG*>(alphafile->Get("alphas"));
	alphas->SetName("alphas");
	
	TFile *backanode = new TFile("cuts_erin/backanode.root");
	TCutG *backtritons = static_cast<TCutG*>(backanode->Get("backtritons"));
	backtritons->SetName("backtritons");
	


	gROOT->Reset();
	std::cout << "\n*********************************************";
	std::cout << "\nTimeshifting SABRE in run#" << i <<  " seg#" << j << " ....";
	if(!skip_evb)
		preprocess(i,j);
	std::cout << "\n******************";
	std::cout << "\nEventbuilding in run#" << i <<  " seg#" << j << " ....";
	if(!skip_evb)
		eventbuild(i,j);
	std::cout << "\n******************";
	std::cout << "\nGenerating histograms..";

   std::cout << "\nlook in segment " << j << " in run " << i << "..."; 
   TFile* builtFile =  new TFile(Form("output/built_preprocessed_trees/builtFile_%d_%d.root",i,j));
   if(!builtFile->IsOpen()) std::cout << "\nBuilt File open error!!";

	TTree* builtTree = static_cast<TTree*>(builtFile->Get("builtTree"));


   builtTree->SetAlias("tdiffF","(delayTimeFL-delayTimeFR)/2.0");
   builtTree->SetAlias("tdiffB","(delayTimeBL-delayTimeBR)/2.0");
   builtTree->SetAlias("tcheckF","(delayTimeFL+delayTimeFR)/2.0-anodeTimeMF");

   TFile *histograms = new TFile(Form("./output/histogram_root/histograms_run=%d,seg=%d.root",i,j),"RECREATE");

   TH2F *anodevsfp = new TH2F("anodevsfp","anodevsfp",800,-500,500,400,0,6000);
   TH2F *scintvsfpL = new TH2F("scintvsfpL","scintvsfpL",800,-500,500,400,0,6000);
   TH2F *scintvsfpR = new TH2F("scintvsfpR","scintvsfpR",800,-500,500,400,0,6000);
   TH2F *anodevsfp_scintalpha = new TH2F("anodevsfp_scintalpha","anodevsfp_scintalpha",800,-500,500,400,0,6000);
   TH2F *anodevsfp_scinttriton = new TH2F("anodevsfp_scinttriton","anodevsfp_scinttriton",800,-500,500,400,0,6000);
   TH2F *anodevsfp_scintdeuteron = new TH2F("anodevsfp_scintdeuteron","anodevsfp_scintdeuteron",800,-500,500,400,0,6000);
   
   TH2F *anodebkvsfpbk = new TH2F("anodebkvsfpbk","anodebkvsfpbk",800,-500,500,400,0,6000);
   TH2F *delayLeftEvsfp = new TH2F("delayLeftEvsfp","delayLeftEvsfp",800,-500,500,400,0,4096);
   TH2F *cathodevsScintL  = new TH2F("cathodevsScintL","cathodevsScintL",400,0,4000,400,0,6000);
   
   TH1F *tdiff_all = new TH1F("tdiff_all","all fp singles",500,-500,500);
   TH1F *tdiff_tritons_delayLE = new TH1F("tritons_delayLE","fp singles gated on tritons using delayLE",500,-500,500);
   TH1F *tdiff_deuterons = new TH1F("tdiff_deuterons","all fp singles gated on deuterons",500,-500,500);
   TH1F *tdiff_tritons = new TH1F("tdiff_tritons","fp singles gated on tritons",500,-500,500);
   TH1F *tdiff_alphas = new TH1F("tdiff_alphas","fp singles gated on alphas",500,-500,500);

   TH2F *EvsChFront = new TH2F("EvsChFront","EvsChFront",144,0,143,800,0,5000);
   TH2F *EvsChBack = new TH2F("EvsChBack","EvsChBack",144,0,143,800,0,5000);
   //TH2F *anodebackvsfp = new TH2F("anodebackvsfp","anodevsfp",800,-700,700,400,0,4000);
   //TH2F *anodevsxavg = new TH2F("anodevsxavg","anodevsxavg",800,-700,700,400,0,4000);
   TH1F *fpsabre_dt = new TH1F("fpsabre_dt","fpsabre_dt",200,-5000,5000);
   TH1F *fpsabre_dt_select = new TH1F("fpsabre_dt_select","fpsabre_dt_select",200,-5000,5000);

   TH2F *fpsabrecoinc = new TH2F("fpsabrecoinc","fpsabrecoinc",800,-500,500,1024,0,2047);
   TH2F *fpsabrecoinc_t = new TH2F("fpsabrecoinc_t","fpsabrecoinc_t",800,-500,500,1024,0,2047);
   TH2F *fpsabrecoinc_d = new TH2F("fpsabrecoinc_d","fpsabrecoinc_d",800,-500,500,1024,0,2047);
   TH2F *fpsabrecoinc_a = new TH2F("fpsabrecoinc_a","fpsabrecoinc_a",800,-500,500,1024,0,2047);
   
   //TH1F *xavg = new TH1F("xavg1dhist","xavg",500,-500,500);
   //TH1F *xavg2 = new TH1F("xavg1dhist2","xavg2",500,-500,500);
   //TH2F *xavg_vs_theta = new TH2F("xavg_vs_theta","xavgvstheta",500,-500,500,600,-3,3);
   //TH2F *xavg_vs_phi = new TH2F("xavg_vs_phi","xavgvsphi",500,-500,500,600,-3,3);
   //TH2F *cathodevsfp = new TH2F("cathodevsfp","cathodevsfp",800,-700,700,400,0,4000);
   
   FocalPlane *FP = new FocalPlane();
   builtTree->SetBranchAddress("delayTimeFL",&FP->delayTimeFL);
   builtTree->SetBranchAddress("delayTimeFR",&FP->delayTimeFR);
   builtTree->SetBranchAddress("delayTimeBL",&FP->delayTimeBL);
   builtTree->SetBranchAddress("delayTimeBR",&FP->delayTimeBR);
   builtTree->SetBranchAddress("anodeTimeMF",&FP->anodeTimeMF);
   builtTree->SetBranchAddress("anodeTimeBF",&FP->anodeTimeBF);
   builtTree->SetBranchAddress("anodeTimeMB",&FP->anodeTimeMB);
   builtTree->SetBranchAddress("scintTimeL",&FP->scintTimeL);//Broken channel
   builtTree->SetBranchAddress("scintTimeR",&FP->scintTimeR);
   builtTree->SetBranchAddress("cathodeTime",&FP->cathodeTime);

   builtTree->SetBranchAddress("delayShortFL",&FP->delayShortFL);
   builtTree->SetBranchAddress("delayShortFR",&FP->delayShortFR);
   builtTree->SetBranchAddress("delayShortBL",&FP->delayShortBL);
   builtTree->SetBranchAddress("delayShortBR",&FP->delayShortBR);
   builtTree->SetBranchAddress("anodeShortMF",&FP->anodeShortMF);
   builtTree->SetBranchAddress("anodeShortBF",&FP->anodeShortMF);
   builtTree->SetBranchAddress("anodeShortMB",&FP->anodeShortMB);
   builtTree->SetBranchAddress("scintShortL",&FP->scintShortL);
   builtTree->SetBranchAddress("scintShortR",&FP->scintShortR);
   builtTree->SetBranchAddress("cathodeShort",&FP->cathodeShort);


   builtTree->SetBranchAddress("delayLongFL",&FP->delayLongFL);
   builtTree->SetBranchAddress("delayLongFR",&FP->delayLongFR);
   builtTree->SetBranchAddress("delayLongBL",&FP->delayLongBL);
   builtTree->SetBranchAddress("delayLongBR",&FP->delayLongBR);
   builtTree->SetBranchAddress("anodeLongMF",&FP->anodeLongMF);
   builtTree->SetBranchAddress("anodeLongBF",&FP->anodeLongBF);
   builtTree->SetBranchAddress("anodeLongMB",&FP->anodeLongMB);
   builtTree->SetBranchAddress("scintLongL",&FP->scintLongL);
   builtTree->SetBranchAddress("scintLongR",&FP->scintLongR);
   builtTree->SetBranchAddress("cathodeLong",&FP->cathodeLong);

   builtTree->SetBranchAddress("MaxSabreFrontLong",&FP->MaxSabreFrontLong);
   builtTree->SetBranchAddress("MaxSabreFrontTime",&FP->MaxSabreFrontTime);
   builtTree->SetBranchAddress("MaxSabreFrontCh",&FP->MaxSabreFrontCh);
   builtTree->SetBranchAddress("MaxSabreBackLong",&FP->MaxSabreBackLong);
   builtTree->SetBranchAddress("MaxSabreBackTime",&FP->MaxSabreBackTime);
   builtTree->SetBranchAddress("MaxSabreBackCh",&FP->MaxSabreBackCh);

   builtTree->SetBranchAddress("xavg",&FP->xavg);
   builtTree->SetBranchAddress("yavg",&FP->yavg);
   builtTree->SetBranchAddress("theta",&FP->theta);
   builtTree->SetBranchAddress("phi",&FP->phi);
   builtTree->SetBranchAddress("plastic_esum",&FP->plastic_esum);
   builtTree->SetBranchAddress("rf_scint_time",&FP->rf_scint_time);


   builtTree->SetMaxVirtualSize(4000000000);
   builtTree->LoadBaskets(4000000000);
   builtTree->BuildIndex("0","anodeTimeMF");


//Reference for sorting : https://root-forum.cern.ch/t/sorting-a-tree/12560/4
//   compassTree->BuildIndex("0","Timestamp");   // order tree by Timestamp... 


//    Double_t front2mm = 0.545;
//    Double_t back2mm = 0.51;

    Double_t front2mm = 1.0;//0.545;
    Double_t back2mm = 1.0;//0.51;
    
    //total entries in tree to loop over:
    Int_t nentries = builtTree->GetEntries();
	
    for (Long64_t k = 0; k < nentries; k++)
	{
		builtTree->GetEntry(k);
        //FOCAL PLANE TIMING ITEMS-----------------------------------------------------
        
        //calculate FP time differences:
        Double_t FrontTdiff = 0;  Double_t BackTdiff=0;
        Double_t FrontTdiffT = 0;  Double_t BackTdiffT=0;
		Double_t FrontAveT = 0;	Double_t FrontAveTmm = 0;
		Double_t Scint = 0; Double_t Cathode_PSD = 0; 
        
        //FP in ns
        FrontTdiffT = (FP->delayTimeFL-FP->delayTimeFR)/2.0;
        BackTdiffT = (FP->delayTimeBL-FP->delayTimeBR)/2.0;
        
        //FP in mm
        FrontTdiff = FrontTdiffT*front2mm;
        BackTdiff = BackTdiffT*back2mm;
        
		//FP averaged time: 
		FrontAveT = (FP->delayTimeFL+FP->delayTimeFR)/2.0;
		FrontAveTmm = FrontAveT*front2mm; 

		if(FP->delayTimeBR>0 && FP->delayTimeBL>0)
		{
			if(FP->anodeLongMB>0)
				{anodebkvsfpbk->Fill(BackTdiff,FP->anodeLongMB);}
		}

        //Fill FP time difference histograms:
        if(FP->delayTimeFR > 0 && FP->delayTimeFL > 0 && FP->delayTimeBR > 0 && FP->delayTimeBL > 0)
		{     //empty events are filled with digits 0 or less on digitizers

			//std::cout << "\n" << FP->delayTimeFL;
            tdiff_all->Fill(FrontTdiff);          //FP in ns            
			
			//front delay line gated on alphas, tritons, and deuterons
			if(deuterons->IsInside(BackTdiff,FP->anodeLongMB))
			{	tdiff_deuterons->Fill(FrontTdiff);}
			if(tighttriton->IsInside(FP->cathodeLong,FP->scintLongL))
			{	tdiff_tritons->Fill(FrontTdiff);}
			if(alphas->IsInside(BackTdiff,FP->anodeLongMB))
			{	tdiff_alphas->Fill(FrontTdiff);}
			if(FP->anodeLongMF>0)
			{
			
			//anode energy v front delay line gated on particle groups
				anodevsfp->Fill(FrontTdiff,FP->anodeLongMF);
				if(alphas->IsInside(BackTdiff,FP->anodeLongMB))
					{anodevsfp_scintalpha->Fill(FrontTdiff,FP->anodeLongMF);}
				if(tighttriton->IsInside(FP->cathodeLong,FP->scintLongL))
					{anodevsfp_scinttriton->Fill(FrontTdiff,FP->anodeLongMF);}
				if(deuterons->IsInside(BackTdiff,FP->anodeLongMB))
					{anodevsfp_scintdeuteron->Fill(FrontTdiff,FP->anodeLongMF);}
			}
			if(FP->delayLongFL>0)
			{
				delayLeftEvsfp->Fill(FrontTdiff,FP->delayLongFL);
				if(tighttriton->IsInside(FP->cathodeLong,FP->scintLongL))
					tdiff_tritons_delayLE->Fill(FrontTdiff);
			}
       }
       
       //cathode v scint energies: ungated and gated
       if(FP->cathodeLong>0 && FP->scintLongL>0 && FP->delayTimeBR > 0 && FP->delayTimeBL > 0)
       {
           cathodevsScintL->Fill(FP->scintLongL,FP->cathodeLong);
      
       }
       
       //scintillator v front focal plane 
       if(FP->anodeLongMF>0)
       {
       		scintvsfpR->Fill(FrontTdiff,FP->scintLongR);
       		scintvsfpL->Fill(FrontTdiff,FP->scintLongL);
       }
    
		//SABRE histograms
		if(FP->MaxSabreFrontLong>0 && FP->MaxSabreFrontCh>-1){
			EvsChFront->Fill(FP->MaxSabreFrontCh,FP->MaxSabreFrontLong);
		}
		if(FP->MaxSabreBackLong>0 && FP->MaxSabreBackCh>-1){
			EvsChBack->Fill(FP->MaxSabreBackCh,FP->MaxSabreBackLong);
		}
		if(
			FP->MaxSabreFrontLong>0 && FP->MaxSabreFrontCh > -1 
			&& FP->delayTimeFL>0 && FP->delayTimeFR>0 
			&& FP->delayTimeBL>0 && FP->delayTimeBR>0
			&& (FP->MaxSabreFrontTime - FP->anodeTimeMF)>0 
			&& (FP->MaxSabreFrontTime - FP->anodeTimeMF)<1200
		  )
		{
			fpsabrecoinc->Fill(FrontTdiff,FP->MaxSabreFrontLong);
			if (tighttriton->IsInside(FP->cathodeLong,FP->scintLongL)){
				fpsabrecoinc_t->Fill(FrontTdiff,FP->MaxSabreFrontLong);
			}

			if (deuterons->IsInside(BackTdiff,FP->anodeLongMB)){
				fpsabrecoinc_d->Fill(FrontTdiff,FP->MaxSabreFrontLong);
			}
			
			if (alphas->IsInside(BackTdiff,FP->anodeLongMB)){
				fpsabrecoinc_a->Fill(FrontTdiff,FP->MaxSabreFrontLong);
			}
		}


		if(FP->anodeTimeMF>0 && FP->MaxSabreFrontTime>0 && FP->MaxSabreFrontCh > -1)
		{
			double dtfpsabre = -(FP->anodeTimeMF- FP->MaxSabreFrontTime);
			fpsabre_dt->Fill(FP->MaxSabreFrontTime - FP->anodeTimeMF);
			if(dtfpsabre>0 && dtfpsabre<1200 )
				{fpsabre_dt_select->Fill(FP->MaxSabreFrontTime - FP->anodeTimeMF);}
		}
	}


   auto* legend = new TLegend(0.32,0.16);
//   legend->SetEntrySeparation(4);
   TCanvas* c0 = new TCanvas("c0","c0",0,0,600,400);
   //TCanvas* c0 = new TCanvas();
   c0->cd();
   tdiff_all->SetLineColor(kBlue);
   tdiff_all->Draw("");

   
   tdiff_tritons_delayLE->SetLineColor(kBlack);
   tdiff_tritons_delayLE->Draw("SAME");
  
   tdiff_deuterons->SetLineColor(kRed);
   tdiff_deuterons->Draw("SAME");

   tdiff_tritons->SetLineColor(kMagenta);
   tdiff_tritons->Draw("SAME");

   tdiff_alphas->SetLineColor(kGreen);
   tdiff_alphas->Draw("SAME");

   legend->AddEntry("tdiff_all","fp singles");
   legend->AddEntry("tdiff_protons","fp+proton+sabre_coinc");
   legend->AddEntry("tdiff_deuterons","fp+deuterons+sabre_coinc");
   legend->AddEntry("tdiff_tritons","fp+tritons+sabre_coinc");
   legend->AddEntry("tdiff_alphas","fp+alphas+sabre_coinc");
   legend->Draw("SAME");
   c0->Modified();
   c0->Update();

   TCanvas* c1 = new TCanvas("c1","",0,420,600,400);
   c1->cd();
   //dataTree->Draw("anodeLongMF:tdiffF>>anodevsfp(800,-700,700,400,0,2000)","delayTimeFL>0 && delayTimeFR>0 && anodeLongMF>0 ","colz");
   //anodevsfp = static_cast<TH2F*>(gDirectory->Get("anodevsfp"));
   anodevsfp->Draw("");
   anodevsfp->SetOption("colz");
   c1->Modified();
   c1->Update();
  
   TCanvas* c2 = new TCanvas("c2","",610,0,600,400);
   c2->cd();
   c2->Modified();
   c2->Update();
   if(segID==0)
   {
   	c1->SaveAs(Form("./output/histogram_pdfs/anodef_fpf_with_cuts_run=%d_seg=0.png",runID));
 	c2->SaveAs(Form("./output/histogram_pdfs/anodef_scintR_with_cuts_run=%d_seg=0.png",runID));
   }


   TCanvas* c3 = new TCanvas("c3","",610,420,600,400);
   c3->cd();
   EvsChFront->Draw("");
   EvsChFront->SetOption("colz");
   c3->Modified();
   c3->Update();


   TCanvas* c4 = new TCanvas("c4","",1220,0,600,400);
   c4->cd();
   EvsChBack->Draw("");
   EvsChBack->SetOption("colz");
   c4->Modified();
   c4->Update();

   TCanvas* c5 = new TCanvas("c5","",1220,420,600,400);
   c5->cd();
   fpsabre_dt->SetLineColor(kBlack);
   fpsabre_dt->Draw("");
   fpsabre_dt_select->SetLineColor(kRed);   
   fpsabre_dt_select->Draw("SAME");
   c5->Modified();
   c5->Update();

   TCanvas* c6 = new TCanvas("c6","",600,200,600,400);
   c6->cd();
   fpsabrecoinc->Draw("");
   fpsabrecoinc->SetOption("colz");
   c6->Modified();
   c6->Update();
   
   TCanvas *test = new TCanvas("test","",600,200,600,400);
   anodebkvsfpbk->Draw("");
   anodebkvsfpbk->SetOption("colz");
   test->cd();
   test->Modified();
   test->Update();

   //TCanvas *one = new TCanvas();
   delayLeftEvsfp->SetOption("colz");
//   delayLeftEvsfp->Draw("");

  // scanf("%d",&temp);
   
   anodevsfp_scintalpha->SetOption("colz");
   anodevsfp_scintdeuteron->SetOption("colz");
   scintvsfpL->SetOption("colz");
   scintvsfpR->SetOption("colz");
   
   builtFile->Close();
   histograms->Write();
   histograms->Close();

   std::cout << "..done.";
  // }   
///}

}

