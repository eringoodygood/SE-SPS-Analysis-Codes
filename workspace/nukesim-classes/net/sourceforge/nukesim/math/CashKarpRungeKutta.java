package net.sourceforge.nukesim.math;

import jade.JADE;
import jade.physics.Acceleration;
import jade.physics.Angle;
import jade.physics.Duration;
import jade.physics.Length;
import jade.physics.Quantity;
import jade.physics.Scalar;
import jade.physics.models.StandardModel;
import jade.realtime.PoolContext;
import jade.units.SI;
import jade.util.Text;
import jade.util.TextBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.nukesim.nuclear.NukeUnits;

/**
 * Performs integrations of <code>Differentiable</code> functions while
 * attempting to adapt the step size at each step to achieve a desired numerical
 * accuracy. See section 16.2 of Numerical Recipes in C for more details.
 */
public class CashKarpRungeKutta {

	private final Differentiable derivs;

	private Quantity[] yerr, yout;

	private Quantity hnext, hdid, xnew;

	/**
	 * Defines a new adaptable integrator for the given
	 * <code>Differentiable</code>.
	 * 
	 * @param diff
	 *            the differential relation to integrate
	 */
	public CashKarpRungeKutta(Differentiable diff) {
		derivs = diff;
	}

	/**
	 * 
	 * @return the result of a requested integration
	 * @throws IllegalStateException
	 *             if <code>integrate()</code> has not been invoked yet
	 */
	public Quantity[] getIntegral() {
		if (yout == null) {
			throw new IllegalStateException(
					"Need to call integrate() before requesting the result.");
		}
		return yout;
	}

	/**
	 * Steps y's at x by h accourding to the Differentiable. The results are
	 * stored in yerr and yout.
	 * 
	 * @param x
	 *            independent variable
	 * @param y
	 *            dependent variables
	 * @param h
	 *            step size along x-axis
	 */
	private void step(final Quantity x, final Quantity[] y, final Quantity h) {
		final int n = y.length;
		Quantity[] ytemp = new Quantity[n];
		yout = new Quantity[n];
		yerr = new Quantity[n];
		PoolContext.enter();
		try {
			final Quantity[] dydx = derivs.dydx(x, y);
			for (int i = 0; i < n; i++) {
				ytemp[i] = y[i].add(h.multiply(dydx[i]).multiply(b21));
			}
			final Quantity[] ak2 = derivs.dydx(x.add(h.multiply(a2)), ytemp);
			for (int i = 0; i < n; i++) {
				ytemp[i] = y[i].add(h.multiply(dydx[i].multiply(b31).add(
						ak2[i].multiply(b32))));
			}
			final Quantity[] ak3 = derivs.dydx(x.add(h.multiply(a3)), ytemp);
			for (int i = 0; i < n; i++) {
				final Quantity paren = dydx[i].multiply(b41).add(
						ak2[i].multiply(b42)).add(ak3[i].multiply(b43));
				ytemp[i] = y[i].add(h.multiply(paren));
			}
			final Quantity[] ak4 = derivs.dydx(x.add(h.multiply(a4)), ytemp);
			for (int i = 0; i < n; i++) {
				final Quantity paren = dydx[i].multiply(b51).add(
						ak2[i].multiply(b52)).add(ak3[i].multiply(b53)).add(
						ak4[i].multiply(b54));
				ytemp[i] = y[i].add(h.multiply(paren));
			}
			final Quantity[] ak5 = derivs.dydx(x.add(h.multiply(a5)), ytemp);
			for (int i = 0; i < n; i++) {
				final Quantity paren = dydx[i].multiply(b61).add(
						ak2[i].multiply(b62)).add(ak3[i].multiply(b63)).add(
						ak4[i].multiply(b64)).add(ak5[i].multiply(b65));
				ytemp[i] = y[i].add(h.multiply(paren));
			}
			final Quantity[] ak6 = derivs.dydx(x.add(h.multiply(a6)), ytemp);
			for (int i = 0; i < n; i++) {
				final Quantity outParen = dydx[i].multiply(c1).add(
						ak3[i].multiply(c3)).add(ak4[i].multiply(c4)).add(
						ak6[i].multiply(c6));
				final Quantity errParen = dydx[i].multiply(dc1).add(
						ak3[i].multiply(dc3)).add(ak4[i].multiply(dc4)).add(
						ak5[i].multiply(dc5)).add(ak6[i].multiply(dc6));
				yout[i] = (Quantity) y[i].add(h.multiply(outParen)).toHeap();
				yerr[i] = (Quantity) h.multiply(errParen).toHeap();
			}
		} finally {
			PoolContext.exit();
		}
	}

	private static double a2 = 0.2;

	private static double a3 = 0.3;

	private static double a4 = 0.6;

	private static double a5 = 1.0;

	private static double a6 = 0.875;

	private static double b21 = 0.2;

	private static double b31 = 3.0 / 40.0;

	private static double b32 = 9.0 / 40.0;

	private static double b41 = 0.3;

	private static double b42 = -0.9;

	private static double b43 = 1.2;

	private static double b51 = -11.0 / 54.0;

	private static double b52 = 2.5;

	private static double b53 = -70.0 / 27.0;

	private static double b54 = 35.0 / 27.0;

	private static double b61 = 1631.0 / 55296.0;

	private static double b62 = 175.0 / 512.0;

	private static double b63 = 575.0 / 13824.0;

	private static double b64 = 44275.0 / 110592.0;

	private static double b65 = 253.0 / 4096.0;

	private static double c1 = 37.0 / 378.0;

	private static double c3 = 250.0 / 621.0;

	private static double c4 = 125.0 / 594.0;

	private static double c6 = 512.0 / 1771.0;

	private static double dc1 = c1 - 2825.0 / 27648.0;

	private static double dc3 = c3 - 18575.0 / 48384.0;

	private static double dc4 = c4 - 13525.0 / 55296.0;

	private static double dc5 = -277.0 / 14336.0;

	private static double dc6 = c6 - 0.25;

	/**
	 * Performs a "safe" step, adjusting the step size from what is requested if
	 * necessary.
	 * 
	 * @param x
	 *            independent variable
	 * @param y
	 *            dependent variables
	 * @param htry
	 *            step size to try
	 * @param eps
	 *            fractional numerical accuracy required
	 * @param yscal
	 *            scales for numerical precision
	 */
	private void safeStep(final Quantity x, final Quantity[] y,
			final Quantity htry, double eps, final Quantity[] yscal) {
		double errmax = 0.0;
		boolean tryAgain = true;
		final int n = y.length;
		PoolContext.enter();
		try {
			Quantity h = htry;
			while (tryAgain) {
				step(x, y, h);//affects yout, yerr
				errmax = 0.0;
				for (int i = 0; i < n; i++) {
					errmax = Math.max(errmax, yerr[i].divide(yscal[i]).abs()
							.doubleValue());
				}
				errmax /= eps;
				tryAgain = errmax > 1.0;
				if (tryAgain) {
					Quantity htemp = h.multiply(SAFETY).multiply(
							Math.pow(errmax, PSHRNK));
					if (h.compareTo(h.multiply(0)) >= 0) {
						h = (Quantity) QuantityUtilities.max(htemp, h
								.multiply(0.1));
					} else {
						h = (Quantity) QuantityUtilities.min(htemp, h
								.multiply(0.1));
					}
					if (h.isPossiblyZero()) {
						throw new RuntimeException(
								"Stepsize underflow in safeStep.");
					}
				}
			}
			if (errmax > ERRCON) {
				hnext = (Quantity) h.multiply(SAFETY).multiply(
						Math.pow(errmax, PGROW)).toHeap();
			} else {
				hnext = (Quantity) h.multiply(5).toHeap();
			}
			hdid = (Quantity) h.toHeap();
			xnew = (Quantity) x.add(h).toHeap();
		} finally {
			PoolContext.exit();
		}
	}

	private static double SAFETY = 0.9;

	private static double PGROW = -0.2;

	private static double PSHRNK = -0.25;

	private static double ERRCON = Math.pow(5 / SAFETY, 1 / PGROW);

	private boolean storePoints = false;

	private Quantity dxsav;//approximate storage interval

	private List xp = new ArrayList();

	private List yp = new ArrayList();

	/**
	 * Instruct the integrator to store points during integration whenever the
	 * independent variable increases by at least the given interval.
	 * 
	 * @param interval
	 *            at which to store integration points
	 */
	public synchronized void setStorePoints(Quantity interval) {
		storePoints = true;
		setDxsav(interval);
	}

	private void clearPoints() {
		xp.clear();
		yp.clear();
	}

	/**
	 * Instruct the integrator to not store any points during an integration.
	 *  
	 */
	public synchronized void unsetStorePoints() {
		storePoints = false;
	}

	/**
	 * 
	 * @return whether the integrator is set to store points during integrations
	 */
	public synchronized boolean getStorePoints() {
		return storePoints;
	}

	private synchronized void setDxsav(Quantity x) {
		dxsav = x;
	}

	/**
	 * 
	 * @return the interval at which the integrator will try to stor points
	 *         during integration
	 */
	public synchronized Quantity getReportingInterval() {
		return dxsav;
	}

	int nok, nbad;

	/**
	 * Integrate aiming for the given accuracy, using h1 and hmin to suggest
	 * step sizes. The given <code>Quantity</code>'s are copied to "exact"
	 * local copies. I.e., maximum numerical precision in the values is assumed.
	 * 
	 * @param _x1
	 *            start of range
	 * @param _x2
	 *            end of range
	 * @param _ystart
	 *            initial values
	 * @param eps
	 *            desired accuracy
	 * @param _h1
	 *            suggested first step size
	 * @param _hmin
	 *            minimum step size (may be zero)
	 */
	public void integrate(final Quantity _x1, final Quantity _x2,
			final Quantity[] _ystart, double eps, final Quantity _h1,
			final Quantity _hmin) {
		Quantity xsav = null;
		Quantity x1 = QuantityUtilities.fakeExact(_x1);
		Quantity x2 = QuantityUtilities.fakeExact(_x2);
		Quantity h1 = QuantityUtilities.fakeExact(_h1);
		Quantity hmin = QuantityUtilities.fakeExact(_hmin);
		Quantity x = x1;
		Quantity h = QuantityUtilities.sign(h1, x2.subtract(x1));
		nok = nbad = 0;
		final int nvar = _ystart.length;
		Quantity[] ystart = new Quantity[nvar];
		for (int i = 0; i < nvar; i++) {
			ystart[i] = QuantityUtilities.fakeExact(_ystart[i]);
		}
		Quantity[] tiny = new Quantity[nvar];
		Quantity[] y = new Quantity[nvar];
		System.arraycopy(ystart, 0, y, 0, nvar);
		if (getStorePoints()) {
			clearPoints();
			xsav = x.subtract(getReportingInterval().multiply(2));
		}
		for (int i = 0; i < nvar; i++) {
			tiny[i] = Quantity.valueOf(1.0e-30, y[i].getSystemUnit());
		}
		for (int nstp = 1; nstp <= MAXSTP; nstp++) {
			Quantity[] dydx = derivs.dydx(x, y);
			Quantity[] yscal = new Quantity[nvar];
			for (int i = 0; i < nvar; i++) {
				/*
				 * Scaling used to monitor accuracy. This general-purpose choice
				 * can be modified if need be.
				 */
				yscal[i] = y[i].abs().add(dydx[i].multiply(h).abs()).add(
						tiny[i]);
			}
			if (getStorePoints()
					&& x.subtract(xsav).abs().compareTo(dxsav.abs()) > 0) {
				xp.add(x.toHeap());
				Quantity[] ystor = new Quantity[nvar];
				for (int i = 0; i < nvar; i++) {
					ystor[i] = (Quantity) y[i].toHeap();
				}
				yp.add(ystor);
				xsav = x;
			}
			if (QuantityUtilities.isPositive(x.add(h).subtract(x2).multiply(
					x.add(h).subtract(x1)))) {
				/* If stepsize can overshoot, decrease. */
				h = x2.subtract(x);
			}
			safeStep(x, y, h, eps, yscal);
			/*
			 * I add the next two lines, because I changed safeStep to not muck
			 * with parameter values, unlike the NR in C program. This code
			 * needs the values to be updated, though.
			 */
			x = xnew;
			y = yout;
			if (hdid.approxEquals(h)) {
				nok++;
			} else {
				nbad++;
			}
			if (QuantityUtilities.isNonNegative(x.subtract(x2).multiply(
					x2.subtract(x1)))) {//finished?
				/* yout contains final result */
				if (getStorePoints()) {
					/* Save final step, x and y already on heap. */
					xp.add(x);
					yp.add(y);
				}
				return;
			}
			if (hnext.abs().compareTo(hmin) <= 0) {
				throw new RuntimeException("Step size too small.");
			}
			h = hnext;
		}
	}

	private static int MAXSTP = 10000;

	public Text getResultsText() {
		Text rval = null;
		PoolContext.enter();
		try {
			final TextBuilder tb = TextBuilder.newInstance();
			tb.append("Integration results: \n");
			for (int i = 0; i < yout.length; i++) {
				tb.append(yout[i].toText());
				tb.append('\n');
			}
			tb.append("There were ");
			tb.append(Integer.toString(nok));
			tb.append(" good iterations, and ");
			tb.append(Integer.toString(nbad));
			tb.append(" iterations that had to be retried.\n");
			if (getStorePoints()) {
				tb.append("Stored points, roughly every ");
				tb.append(getReportingInterval().toText());
				tb.append(":\nx\ty's\n");
				Iterator xit = xp.iterator();
				Iterator yit = yp.iterator();
				while (xit.hasNext()) {
					tb.append(xit.next().toString());
					tb.append('\t');
					Quantity[] yshow = (Quantity[]) yit.next();
					for (int i = 0; i < yshow.length; i++) {
						tb.append(yshow[i].toText());
						tb.append('\t');
					}
					tb.append('\n');
				}
			}
			rval = (Text) tb.toText().export();
		} finally {
			PoolContext.exit();
		}
		return rval;
	}

	public static void main(String[] args) {
		JADE.initialize();
		StandardModel.select();
		CashKarpRungeKutta ckrk = new CashKarpRungeKutta(new Differentiable() {
			public Quantity[] dydx(Quantity t, Quantity[] x) {
				final int n = x.length;
				final Quantity[] rval = new Quantity[n];
				for (int i = 0; i < n; i++) {
					rval[i] = t.multiply(Acceleration.GRAVITY);
				}
				return rval;
			}
		});
		Quantity hguess = Quantity.valueOf(.01, SI.SECOND);
		ckrk.setStorePoints(hguess);
		final Quantity[] ystart = { Length.ZERO };
		Quantity hmin = Quantity.valueOf(1.0e-30, SI.SECOND);
		ckrk.integrate(Duration.ZERO, Quantity.valueOf(2, SI.SECOND), ystart,
				0.1, hguess, hmin);
		System.out.println(ckrk.getResultsText());
		ckrk = new CashKarpRungeKutta(new Differentiable() {
			public Quantity[] dydx(Quantity x, Quantity[] y) {
				final int n = y.length;
				final Angle angleX = Angle.angleOf(x);
				final Quantity[] rval = new Quantity[n];
				for (int i = 0; i < n; i++) {
					rval[i] = angleX.sine().subtract(angleX.cos());
				}
				return rval;
			}
		});
		hguess = Quantity.valueOf(30, NukeUnits.deg);
		ckrk.setStorePoints(hguess);
		final Quantity[] ystart2 = { Scalar.ZERO };
		hmin = Quantity.valueOf(1e-6, SI.RADIAN);
		final double eps = 0.00001;
		System.out.println("eps = " + eps);
		ckrk.integrate(Angle.ZERO, Quantity.valueOf(180, NukeUnits.deg),
				ystart2, eps, hguess, hmin);
		System.out.print(ckrk.getResultsText());
	}
}