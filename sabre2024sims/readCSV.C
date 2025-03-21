{
    TFile *f = new TFile("2365_13N_3deg_50keVthresh.root");
    
   // TTree *t = new TTree("t","t");
    TTree *t = (TTree*)f->Get("t");

    Int_t det, strip;
    Double_t Edep, inc;
    
    t->SetBranchAddress("det",&det);
    t->SetBranchAddress("strip",&strip);
    t->SetBranchAddress("Edep",&Edep);
    t->SetBranchAddress("inc",&inc);

    
    TH1D *h0 = new TH1D("h","strip 0",1000,0,1.0);
    TH2D *H1 = new TH2D("H1","Sim: Det 0",1000,0,1.0,16,0,15);
    TH2D *H2 = new TH2D("H2","Sim: Det 0",500,1.0,1.5,6,0,5);

    
    for(int i=0; i < t->GetEntries(); i++){
        t->GetEntry(i);
        H2->Fill(inc,det);

        
        if(det == 4){

            H1->Fill(Edep,strip);
        }
        
    }
    
    TCanvas *c0 = new TCanvas("c0","strip 0");
    H1->SetTitle("Sim: Det 4");
    H1->GetXaxis()->SetRangeUser(0.1,0.35);
    H1->Draw();
    
    TCanvas *c1 = new TCanvas("c1","c1");
    H2->SetTitle("Sim: 3 degrees");
    //H2->GetXaxis()->SetRangeUser(0.05,0.35);
    H2->GetXaxis()->SetTitle("Energy (MeV)");
    H2->GetYaxis()->SetTitle("Detector");
    H2->Draw();
    
    
}
