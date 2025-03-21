{
  // Note: make sure that the input data file ("Kabala.csv") is a properly
  // formatted "comma-separated values (CSV)" file.
  // If the "Kabala.csv" file has lines in form "|...;...;...|", first run:
  // sed -i -e '{s/|//g;s/;/,/g}' Kabala.csv # no "|", ";" changed into ","
  TTree *t = new TTree("t", "t");
  t->ReadFile("2365_13N_0_12C_L0_back_0.0mil24MeV_3.0deg.csv", "hit/I:projTheta/D:projPhi/D:CMtheta/D:CMphi/D:labTheta/D:labPhi/D:inc/D:Edep/D:tof/D:det/I:strip/I");
    
    //TFile: write stuff
    TFile *f = new TFile("2365_13N_3deg.root","recreate");
    t->Write();
    f->Write();
    f->Close();
}
