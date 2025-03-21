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
package dwvisser.monte;

/**
 * Random number generator.
 * 
 * @author William Handler
 * @author  Dale Visser
 * @version 1.0 (21 May 2002)
 */
public class RandomNumbers extends Object {
    double [] u =new double[97];
    double c, cd, cm, uni;
    int i97, j97;
    boolean initialised;

    /** 
     * Creates new Random number generator. The seed value Will has
     * always used is 6789754.
     *
     * @param ijkl seed value from 0 to 900000000
     */
    public RandomNumbers(int ijkl) throws Exception{
        int	i, j, k, l, ij, kl;
        int	ii, jj, m;
        double s, t;

        initialised = false;

        if( ijkl < 0 ||  ijkl > 900000000) {
            throw new Exception("The random number seed must have a value "+
            "between 0 and 900 000 000.");
        }

        ij = ijkl / 30082;
        kl = ijkl - 30082 * ij;

        i = mod(ij/177, 177) + 2;
        j = mod(ij    , 177) + 2;
        k = mod(kl/169, 178) + 1;
        l = mod(kl,     169);

        for(ii = 0; ii < 97;ii++){
            s = 0.0;
            t = 0.5;
            for (jj = 0; jj < 24; jj++) {
                m = mod(mod(i*j, 179)*k, 179);
                i = j;
                j = k;
                k = m;
                l = mod(53*l+1, 169);
                if (mod(l*m, 64) >= 32) s = s + t;
                t = 0.5 * t;
            }
            u[ii] = s;
        }

        c = 362436.0 / 16777216.0;
        cd = 7654321.0 / 16777216.0;
        cm = 16777213.0 /16777216.0;
        i97 = 96;
        j97 = 32;
        initialised = true;
        System.out.println("Initialized "+getClass()+" with seed "+ijkl);
    }
    
    public RandomNumbers() throws Exception {
        this(6789754);
    }
    
    public double next() {
	/*if (!initialised) {
	  throw new Exception(getClass().getName()+".next(): not initialized");
        }*/
	uni = u[i97] - u[j97];
	if( uni < 0.0 ) uni = uni + 1.0;
	u[i97] = uni;
	i97 = i97 - 1;
	if(i97 == -1) i97 = 96;
	j97 = j97 - 1;
	if(j97 == -1) j97 = 96;
	c = c - cd;
	if( c < 0.0 ) c = c + cm;
	uni = uni - c;
	if( uni < 0.0 ) uni = uni + 1.0;
	return uni;
    }

    static private int mod(int i, int j) {
        return i % j;
    }

    public static void main(String [] a){
        try {
            RandomNumbers rw=new RandomNumbers();
            for (int i=0; i< 10; i++){
                System.out.println(rw.next());
            }
        } catch (Exception e) {
            System.err.println(e);
        }

    }
}