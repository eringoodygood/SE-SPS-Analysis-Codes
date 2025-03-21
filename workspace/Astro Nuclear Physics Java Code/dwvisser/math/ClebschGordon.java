package dwvisser.math;

/**
 * Copied from NSF libraries clebsh.f
 */
public class ClebschGordon {

	public ClebschGordon() {
		createFactorialArray();
	}

	private static double[] falog = new double[200];
	void createFactorialArray() {
		falog[1] = 0;
		falog[3] = 0;
		for (int j = 5; j <= 175; j += 2) {
			double xx = 0.5 * (j - 1);
			falog[j] = Math.log(xx) + falog[j - 2];
		}
	}

	/**
	 * Calculates the Clebsch-Gordon Coeffiecient, <j1m1j2m2|JM>.
	 */
	public double getCoefficient(
		double j1,
		double m1,
		double j2,
		double m2,
		double J,
		double M) {
		return clebgor(
			(int) Math.round(j1 * 2),
			(int) Math.round(j2 * 2),
			(int) Math.round(m1 * 2),
			(int) Math.round(m2 * 2),
			(int) Math.round(J * 2),
			(int) Math.round(M * 2));
	}

	private double clebgor(int n, int p, int q, int r, int s, int t) {
		int i = 0;
		double w = Math.sqrt((double) (s + 1));
		//checking if triangle condition is satisfied
		double y = tri(n, p, s);
		if (y == 0.0) return 0.0;
		//checking if m1+m2=M, return 0 if not
		if (q+r != t) return 0.0;
		//Checking that for each J,Jz pair:  J >= |Jz|, return 0 if not
		int i1 = n + q;
		int i2 = n - q;
		int i3 = p + r;
		int i4 = p - r;
		int i5 = s + t;
		int i6 = s - t;
		if ((i1 < 0) || (i2 < 0) || (i3 < 0) || (i4 < 0) || (i5 < 0) || (i6 < 0))
			return 0.0;
		double x =
			0.5
				* (falog[i1
					+ 1]
					+ falog[i2
					+ 1]
					+ falog[i3
					+ 1]
					+ falog[i4
					+ 1]
					+ falog[i5
					+ 1]
					+ falog[i6
					+ 1])
				+ trical(n, p, s)
				+ Math.log(w);
		double z = 0.0;
		boolean mflag = false;
		y = falog[i + 1];
		int j = n + p - s - i;
		int l = 0;
		while (true) {
			if (j >= 0) {
				y += falog[j + 1];
				l++;
				if (l == 1) j = n - q - i;
				if (l == 2) j = p + r - i;
				if (l == 3) {
					mflag = true;
					j = s - p + q + i;
				}
				if (l == 4) j = s - n - r + i;
				if (l == 5) {
					int k;
					if ((i % 4) != 0) {
						k = -1;
					} else {
						k = 1;
					}
					z += k * Math.exp(x - y);
					i += 2;
					mflag = false;
					y = falog[i + 1];
					j = n + p - s - i;
					l = 0;
				}
			} else {//j<0
				if (mflag) {
					i += 2;
					mflag = false;
					y = falog[i + 1];
					j = n + p - s - i;
					l = 0;
				} else {
					return z;
				}
			}
		}

		//shouldn't be here
		//System.err.println("Problem in Clebsh-Gordon.  Shouldn't drop out of while-loop\n"+
		//" before returning a value. Returning 0.");
		//return 0.0;
	}

	private double tri(int i, int j, int k) {
		double rval = 1.0;
		int m1 = i + j - k;
		int m2 = i - j + k;
		int m3 = j + k - i;
		int m4 = i + j + k + 2;
		if ((m1 < 0) || (m2 < 0) || (m3 < 0) || (m4 < 0))
			rval = 0.0;
		return rval;
	}

	private double trical(int i, int j, int k) {
		int m1 = i + j - k;
		int m2 = i - j + k;
		int m3 = j + k - i;
		int m4 = i + j + k + 2;
		return 0.5 * (falog[m1 + 1] + falog[m2 + 1] + falog[m3 + 1] - falog[m4 + 1]);
	}

	public String getDiracNotation(
		double j1,
		double m1,
		double j2,
		double m2,
		double J,
		double M) {
		return "<" + nice(j1) + " " + nice(j2) + "; " + nice(m1) +
		 " " + nice(m2) + "| " + nice(J) + " " + nice(M) + ">";
	}
	
	private String nice(double x){
		int numerator = (int)Math.round(x/0.5);
		if ((numerator%2)==0){
			return Integer.toString(numerator/2);
		} else {
			return numerator+"/2";
		}
	}

	static public void printTable(double j1, double j2){
		ClebschGordon cg = new ClebschGordon();
		double Jmax = j1+j2;
		System.out.println("j1 = " + cg.nice(j1) + ", j2 = " + 
		cg.nice(j2));
		for (double J=Jmax; J >= 0.0; J--){
		System.out.println("--- J = "+cg.nice(J)+" ---");
		for (double M = J; M >= -J; M--) {
			for (double m1 = j1; m1 >= -j1; m1 -= 1) {
				for (double m2 = j2; m2 >= -j2; m2 -= 1) {
					double coeff = cg.getCoefficient(j1, m1, j2, m2, J, M);
					if (coeff != 0.0) {
						System.out.println(cg.getDiracNotation(j1, m1, j2, m2, J, M)+ 
						" = " + coeff);
					}
				}
			}
		}
		}
		System.out.println("---");

	}
		

	static public void main(String[] args) {
		System.out.println("Welcome to ClebschGordon.");
		System.out.println("I calculate <j1 j2; m1 m2| J M>.\n");
		printTable(0.5,0);
		printTable(0.5,1);
		System.out.println("Done.");
	}

}