/***************************************************************
 * Nuclear Simulation Java Class Libraries
 * Copyright (C) 2003 Yale University
 * 
 * Original Developer
 *     Dale Visser (dale@visser.name)
 * 
 * OSI Certified Open Source Software
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the University of Illinois/NCSA 
 * Open Source License.
 * 
 * This program is distributed in the hope that it will be 
 * useful, but without any warranty; without even the implied 
 * warranty of merchantability or fitness for a particular 
 * purpose. See the University of Illinois/NCSA Open Source 
 * License for more details.
 * 
 * You should have received a copy of the University of 
 * Illinois/NCSA Open Source License along with this program; if 
 * not, see http://www.opensource.org/
 **************************************************************/
package cmdeibel.nuclear;

import dwvisser.math.DiffEquations;
import dwvisser.math.RungeKutta4;
import dwvisser.math.UncertainNumber;
import java.io.Serializable;
import javax.swing.JOptionPane;


/**
 * Class for calculating energy losses of ions in given absorber.
 * 
 * @author Dale Visser
 * @version 1.2
 */
public final class EnergyLoss implements DiffEquations, Serializable{
    
	/**
     * Conversion factor from MeV/c<sup>2</sup> to amu.
     */
    public final static double MEV_TO_AMU = 0.0010735438521;
    
    /**
     * Conversion factor from amu to keV/c<sup>2</sup>.
     */
    public final static double AMU_TO_KEV = 1/(MEV_TO_AMU/1000);
    
    /**
     * The unitless fine structure constant (approximately 1/137).
     */
    public final static double FINE_STRUCTURE=7.297352533e-3;
    
    /**
     * e<sup>4</sup>, which is defined as 4 pi alpha<sup>2</sup> in
     * the metric used.
     */
    public final static double E_TO_4 = Math.pow(4*Math.PI*FINE_STRUCTURE,2);
    
    /**
     * 10<sup>-24</sup> times Avagodro's number, used for conversions.
     */
    public final static double AVAGADRO = 0.60221367;
    
    private static final double MAX_STEP_FRAC = 0.001;
    
	private final EnergyLossData data=EnergyLossData.instance();
    
    /**
     * The fractional amount of a chemical element in an absorber.
     */
    private double [] fractions;
    
    /**
     * The current absorber object to be used in calculations.
     */
    private Absorber absorber;
    
    /**
     * The atomic numbers of the components of the absorber.
     */
    private int [] Z;
    
    /**
     * The thickness of the absorber in micrograms/cm^2.
     */
    private double thickness;
    
    private Nucleus projectile;    
    
    /**
     * Create an energy loss calculator associated with the given 
     * absorber.
     * 
     * @param a material that energy losses will be calculated in
     */
    public EnergyLoss(Absorber a) {
        setAbsorber(a);
    }
    
    /**
     * Called whenever one wants to change the absorber in this 
     * object.
     *
     * @param a the new absorber
     */
    public void setAbsorber(Absorber a){
    	synchronized(this){
        	absorber = a;
        	Z = absorber.getElements();
        	fractions = absorber.getFractions();
        	thickness = absorber.getThickness();
        }
    }
    
	/**
	 * Returns the absorber object used by this instance.
	 * 
	 * @return the absorbing material specification
	 */
	public Absorber getAbsorber(){
		return absorber;
	}
        
    /**
     * Returns the total stopping power, using the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @throws NuclearException if the energy is greter than 100 
     * MeV/u
     * @return total stopping power in keV/[&#181;g/cm&#178;]
     */
    public double getStoppingPower(Nucleus p, double energy)
    throws NuclearException {
    	setProjectile(p);
        return getElectronicStoppingPower(p,energy)
        + getNuclearStoppingPower(p,energy);
    }
    
    /**
     * Gets dx/dE, the inverse of the stopping power, which is used
     * for calculating the range.
     *
     * @param energy initial energy in MeV
     * @param x any array, since it isn't used here
     * @return dx/dE in [&#181;g/cm&#178;]/MeV
     * @see dwvisser.math.DiffEquations#dydx(double,double[])
     */
    public double [] dydx(double energy, double [] x){
        final double [] rval = new double[1];
        try {
            rval[0] = 1.0/getStoppingPower(projectile,energy);
        } catch (NuclearException ne) {
    		JOptionPane.showMessageDialog(null, ne.getMessage(), 
    		getClass().getName(), JOptionPane.ERROR_MESSAGE);
        }
        return rval;
    }
    
    /**
     * Returns the range in the absorber material specified in this
     * instance of <code>EnergyLoss</code>.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @return the range in &#181;g/cm&#178;
     */
    public UncertainNumber getRange(Nucleus p, double energy){
        setProjectile(p);
        final double stepE = .001; //step size in MeV
        final double [] temp = {0.0};
        final int numSteps=Math.min(10,(int)(energy/stepE));
        final RungeKutta4 rk=new RungeKutta4(this);
        final double tempOut=rk.dumbIntegral(energy/numSteps,energy,temp,
        numSteps)[0];
        return new UncertainNumber(tempOut,
        tempOut*getFractionError(projectile,energy)) ;
    }
    
    /**
     * Returns the total energy loss in keV.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and
     * Ranges of Ions in Matter" by Ziegler
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @param theta angle of incidence (from normal in radians)
     * @return energy loss in keV
     */
    public double getEnergyLoss(Nucleus p, double energy, 
    double theta) {
    	setProjectile(p);
        double xc=0;
        final double x=thickness/Math.cos(theta);
        double ef=energy;
        boolean stop=false;
        double h=0.25*x;
        final double eStopped = 0.05*energy;
        /* Take care of case where given absorber has no thickness. */
        if (this.absorber.getThickness() == 0.0){
        	return 0.0;
        }
        try {
           while (!stop){
                if (Math.abs(getStoppingPower(projectile,ef)*h/(ef*1000)) <
                MAX_STEP_FRAC) {//step not too big
                    if ((h+xc) > x) {//last step
                        stop = true;
                        h = x-xc;//remaining thickness
                        ef -= getStoppingPower(projectile,ef)*h/1000;
                        if (ef <= eStopped) {
                        	return energy*1000;
                        }
                    } else {
                        xc += h;
                        ef -= getStoppingPower(projectile,ef)*h/1000;
                        if (ef <= eStopped) {
                        	return energy*1000;
                        }
                    }
                } else {//step size too big, adjust lower
                    h /= 2.0;
                }
            }
        } catch (NuclearException ne) {
            ne.printStackTrace();
            return -1.0;
        }
        return (energy-ef)*1000;
    }
    
    /**
     * Returns initial ion energy in MeV.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @param theta angle of incidence (from normal in radians)
     * @return initial energy in MeV the ion would have had to emerge
     * with the given energy
     */
    public double reverseEnergyLoss(Nucleus p, double energy, 
    double theta) {
    	setProjectile(p);
        final double x=thickness/Math.cos(theta);
        double xc=0;
        double ei=energy;
        boolean stop=false;
        double h=0.25*x;
        try {
            double deltaE = getStoppingPower(projectile,ei)*h/1000;
            while (!stop){
                if (Math.abs(deltaE/ei) < MAX_STEP_FRAC ) {//step not too big
                    if ((h+xc) > x) {//last step
                        stop = true;
                        h = x-xc;//remaining thickness   
                        deltaE = getStoppingPower(projectile,ei)*h/1000;  
                        ei += deltaE;
                    } else {
                        xc += h;
                        deltaE = getStoppingPower(projectile,ei)*h/1000;
                        ei += deltaE;
                    }
                } else {//step size too big, adjust lower
                    h /= 2.0;
                    deltaE = getStoppingPower(projectile,ei)*h/1000;
                }
            }
        } catch (NuclearException ne) {
            ne.printStackTrace();
            ei = -1.0;
        }
        return ei;
    }
    
    /**
     * Returns the light output produced in a plastic scintillator
     * by an ion losing energy in it.  The units are arbitrary,
     * normalized to L.O.=30 for an 8.78 MeV alpha.  The formula
     * for this comes from NIM 138 (1976) 93-104, table 3, row I. 
     * The formula is implemented in the private method, getL.
     * The formula is technically only good for 0.5 MeV/u to 15
     * MeV/u and assumes complete stopping, so if there is partial
     * energy loss, we take the difference of the light output
     * for the initial and final energies.  The accuracy is +/- 20%.
     *
     * @return light output scaled to 30 for an 8.78 MeV &#x3b1;
     * @param p incident projectile
     * @param energy in MeV
     * @param theta incidence angle in radians
     */
    public double getPlasticLightOutput(Nucleus p, double energy,
    double theta){
    	/* the next line sets the projectile */
    	final double efinal=energy-1000*getEnergyLoss(p,energy,theta);
    	final boolean stopped=efinal < 0.0;
    	final double topL=getL(p,energy);
    	final double rval= stopped ? topL : topL-getL(p,efinal);
    	return rval;
    }
    
    private double getL(Nucleus p, double energy){
    	return 4.0*Math.pow(p.Z*p.A,-0.63)*Math.pow(energy,1.62);
    }
    
    /**
     * Returns the total energy loss in keV at normal incidence. Uses
     * the absorber information already set in the current instance
     * of <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and
     * Ranges of Ions in Matter" by Ziegler.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @return energy loss in keV
     */
    public double getEnergyLoss(Nucleus p, double energy){
        return getEnergyLoss(p, energy, 0.0);//sets projectile
    }
    
    
    /**
     * Returns the total energy loss in keV.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and 
     * Ranges of Ions in Matter" by Ziegler
     *
     * @param ion the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @return energy loss in keV
     */
    public UncertainNumber getEnergyLossUnc(Nucleus ion, double energy){
    	/* The next line sets the projectile */
        final double eloss = getEnergyLoss(ion,energy);//in keV
        final boolean notStopped= eloss != energy*1000.0;
        return notStopped ? new UncertainNumber(eloss,
		eloss*getFractionError(ion,energy-eloss/1000)) :
		new UncertainNumber(eloss,0.0);
    }
    
    /**
     * Returns the fractional error in energy loss.  Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.  See p. 16 of v.3 of "Stopping and 
     * Ranges of Ions in Matter" by Ziegler
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @return the relative error bar for the stopping power
     */
    private double getFractionError(Nucleus p, double energy) {
    	/* no need to set projectile */
        final double mass = p.getMass().value;
        final double ea=energy/(mass*MEV_TO_AMU);//MeV/amu
        double rval=0.025;
        if (ea <= 2.0) {
            if (absorber instanceof Gas) {
                rval = 0.1;
            } else {//solid
                rval = 0.05;
            }
        }
        return rval;
    }
    
    private double [] getLowEnergyES(double ea){
    	final double [] stopH=new double[Z.length];
    	final double sea=Math.sqrt(ea);
        for (int i = 0; i < Z.length; i++) {
            stopH[i]=data.COEFFS[0][Z[i]]*sea;
        }
        return stopH;
    }
    
    private double [] getMedEnergyES(double ea){
    	final double [] stopH=new double[Z.length];
		for (int i = 0; i < Z.length; i++) {
			final double sLo=data.COEFFS[1][Z[i]]*Math.pow(ea,0.45);
			final double sHi=data.COEFFS[2][Z[i]]/ea*
			Math.log(1.0+data.COEFFS[3][Z[i]]/ea+data.COEFFS[4][Z[i]]*ea);
			stopH[i]=sLo*sHi/(sLo+sHi);
		}
		return stopH;
    }
    
    private double [] getHiEnergyES(double mass, double energy, double ea){
    	final double [] stopH=new double[Z.length];
		/* beta squared */
		final double beta2=(energy*(energy+2.*mass))/Math.pow(energy+mass,2.0);
		for (int i = 0; i < Z.length; i++) {
			stopH[i]=data.COEFFS[5][Z[i]]/beta2;
			double logTerm = data.COEFFS[6][Z[i]]*beta2/(1.0-beta2) - beta2 -
			data.COEFFS[7][Z[i]];
			for (int j=1;j<=4;j++){
				logTerm += data.COEFFS[7+j][Z[i]] * Math.pow(Math.log(ea),j);
			}
			stopH[i] *= Math.log(logTerm);
		}
		return stopH;
    }
    
    private double [] getHydrogenEs(double mass, double energy, double ea){
    	final double [] stopH;
        if (ea <= 10.0) {
        	stopH=getLowEnergyES(ea);
        } else if (ea > 10.0 && ea <= 1000.0) {
			stopH=getMedEnergyES(ea);
        } else{// ea > 1000.0 && ea <= 100000.0
        	stopH=getHiEnergyES(mass,energy,ea);
        }
        return stopH;
    }
     
    /**
     * Returns the stopping power due to collisions with electrons.
     * Uses the absorber information already set in 
     * <code>EnergyLoss</code>. See p. 16 in Andersen & Ziegler,
     * "The Stopping and Ranges of Ions in Matter", volume 3.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @throws NuclearException if the ion energy is greater than 100
     * AMeV
     * @return stopping power in keV/[&#181;g/cm&#178]
     */
    public double getElectronicStoppingPower(Nucleus p, double energy)
    throws NuclearException {
    	setProjectile(p);
        /* First part - calculate stopping power for 1H at this energy
         * Find Energy in keV per amu of projectile */
        final double mass = projectile.getMass().value;
        final double ea=energy*1000.0/(mass*MEV_TO_AMU);//keV/amu
        /* Find contributions of components */
        final double [] stopH;
       	if (ea>100000.0) {
        	final String s1=
        	".getElectronicStoppingPower(): E/A in keV/amu  > 100000: ";
        	final StringBuffer message=new StringBuffer(
        	getClass().getName()).append(s1).append(ea);
        	throw new NuclearException(message.toString());
        } else {
        	stopH=getHydrogenEs(mass, energy, ea);
        }
        if (projectile.Z>1){//adjust if not hydrogen
        	adjustForZratio(stopH,ea);
        }
        return sumComponents(stopH);
    }
    
    private double sumComponents(double [] stopH){
		double stotal=0.0;
		double conversion=0;
		for (int i=0;i<Z.length;i++){
			conversion += fractions[i]*data.NATURALWEIGHT[Z[i]];
			stotal += fractions[i]*stopH[i];
		}
		stotal *= AVAGADRO/conversion; 
		return stotal;   	
    }

	private void adjustForZratio(double [] stopH, final double ea){
		for (int i=0;i<Z.length;i++){
			double zratio;//3 diff cases He,Li,Heavy Ion see p.9, v.5
			if (projectile.Z<4){
				final double lea = Math.log(ea);
				double expterm=Math.pow(7.6-lea,2.0);
				final double gamma = 1.+(.007+.00005*Z[i])*
				Math.exp(-expterm);
				if (projectile.Z==2){//He ion
					expterm=.7446+.1429*lea+
					.01562*lea*lea-.00267*Math.pow(lea,3.0)+
					1.325e-06*Math.pow(lea,8.0);
					zratio=2.*gamma*(1.-Math.exp(-expterm));
				} else {//Li ion
					zratio=3.*gamma*(1.-Math.exp(-(.7138+.002797*ea+
					1.348e-06*ea*ea)));
				}
			} else {// projectile.Z>= 4
				final double b=.886*Math.sqrt(ea/25.)/
				Math.pow(projectile.Z,2./3.);
				final double a=b+.0378*Math.sin(Math.PI*b/2.);
				zratio=1-Math.exp(-a)*(1.034-.1777*Math.exp(-.08114*
				projectile.Z));
				zratio *= projectile.Z;
			}
			stopH[i] *= zratio*zratio;
		}
	}
	
	private synchronized void setProjectile(Nucleus ion){
		projectile=ion;
	}
    
    /**
     * Returns the stopping power due to nuclear collisions.
     * Uses the absorber
     * information already set in the current instance of
     * <code>EnergyLoss</code>.
     *
     * @param p the ion being stopped
     * @param energy the ion kinetic energy in MeV
     * @return stopping power in keV/[&#181;g/cm&#178]
     */
    public double getNuclearStoppingPower(Nucleus p, double energy){
    	setProjectile(p);
        final int z1 = projectile.Z;
        final double keV = energy*1000.0;
        final double m1 = projectile.getMass().value*MEV_TO_AMU;
        final double two3 = 2.0/3.0;
        double stotal = 0.0;
        for (int i = 0; i < Z.length; i++) {
            final int z2 = Z[i];
            final double m2=data.NATURALWEIGHT[z2];
            /* Ziegler v.5 p. 19, eqn. 17 */
            final double x = (m1+m2)*Math.sqrt(Math.pow(z1,two3)+
            Math.pow(z2,two3));
            final double eps = 32.53*m2*keV/(z1*z2*x);
            // Ziegler v.5 p. 19, eqn. 15
            final double sn1 = 0.5*Math.log(1.0+eps)/
            (eps+0.10718*Math.pow(eps,0.37544));
            /* Ziegler v.5 p. 19, eqn. 16, stopping power in eV/1e15 
             * atoms/cm^2 */
            final double sn2 = sn1*8.462*z1*z2*m1/x;
            final double conversion=AVAGADRO/m2;
            final double stemp=fractions[i]*sn2*conversion;
            stotal += stemp;
        }
        return stotal;
    }
}
