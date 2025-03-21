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
package dwvisser.analysis;
import dwvisser.math.Matrix;

import dwvisser.math.MathException;

public class GJENice{

    private Matrix coefficients;
    private Matrix b;
    private Matrix inverse; // n by n
    private Matrix results; // n by m

    private int [] RowIndex;
    private int [] ColumnIndex;
    private int [] Pivot;

    private int n;
    private int m;

    private int PivotRow;
    private int PivotColumn;


    /**
     * Takes a coefficient matrix and a result matrix (A and b in A*x=b)
     * and immediately finds A[inverse] and x.  The object can then be
     * queried for these.
     */
    public GJENice(Matrix InputMatrix,Matrix InputVectors) throws MathException{
        this.inverse=new Matrix(InputMatrix);
        this.coefficients=new Matrix(InputMatrix);
        this.results=new Matrix(InputVectors);
        this.b=new Matrix(InputVectors);
        this.n=InputMatrix.rows;
        this.m=InputVectors.columns;
        if (n!=InputVectors.rows) {
            System.err.println(n+" not equal to "+InputVectors.rows+"!!!");
        }
        RowIndex=new int[n];
        ColumnIndex=new int[n];
        Pivot=new int[n];
        go();
    }

    private void go() throws MathException {
        clearPivot();
        findPivots();
    }

    private void clearPivot(){
        int i;

        for (i=0;i<n;i++){
            Pivot[i]=0;
        }
    }

    private void findPivots() throws MathException{
        int k;
        int j;
        int ll;
        int l;
        int i;
        double big;
        double PivotInverse;
        double dummy;

        for (i=0;i<n;i++){
            big=0.0;
            for (j=0;j<n;j++){
                if (Pivot[j] != 1){
                    for(k=0;k<n;k++){
                        if (Pivot[k]==0) {
                            if (Math.abs(inverse.element[j][k])>=big) {
                                big = Math.abs(inverse.element[j][k]);
                                PivotRow=j;
                                PivotColumn=k;
                            }
                        } else if (Pivot[k] > 1) {
                            throw new MathException("GJE: Singular Matrix-1");
                        }
                    }
                }
            }
            Pivot[PivotColumn]++;
            if (PivotRow != PivotColumn){//put pivot element on diagonal
                inverse.permute(PivotRow,PivotColumn,'r');
                results.permute(PivotRow,PivotColumn,'r');
            }
            RowIndex[i]=PivotRow;
            ColumnIndex[i]=PivotColumn;
            if (inverse.element[PivotColumn][PivotColumn]==0.0){
                throw new MathException(getClass().getName()+
                ".findPivots(): Singular Matrix\n"+coefficients.toString(3));
            }
            PivotInverse=1.0/inverse.element[PivotColumn][PivotColumn];
            inverse.element[PivotColumn][PivotColumn]=1.0;
            inverse.rowMultiply(PivotColumn,PivotInverse);
            results.rowMultiply(PivotColumn,PivotInverse);
            for(ll=0;ll<n;ll++){
                if (ll!= PivotColumn){
                    dummy=inverse.element[ll][PivotColumn];
                    inverse.element[ll][PivotColumn]=0.0;
                    for (l=0;l<n;l++){
                        inverse.element[ll][l] = inverse.element[ll][l] -
                        inverse.element[PivotColumn][l]*dummy;
                    }
                    for (l=0;l<m;l++){
                        results.element[ll][l] = results.element[ll][l] -
                        results.element[PivotColumn][l]*dummy;
                    }
                }
            }
        }
        //Now unscramble the permuted columns.
        for(l=n-1;l>=0;l--){
            if (RowIndex[l] != ColumnIndex[l]){
                inverse.permute(RowIndex[l],ColumnIndex[l],'r');
            }
        }
    }

    public Matrix getInverse(){
        return inverse;
    }
    public Matrix getSolution(){
        return results;
    }

    public static void main(String [] argv){
        Matrix a=new Matrix(2,2);
        Matrix b=new Matrix(2,1);
        a.element[0][0]=1.0;
        a.element[0][1]=2.0;
        a.element[1][0]=1.0;
        a.element[1][1]=4.0;
        b.element[0][0]=5.0;
        b.element[1][0]=8.0;
        System.out.println(a.toString(2));
        System.out.println(b.toString(2));
        try{
            GJENice gje = new GJENice(a,b);
            System.out.println(gje.getInverse().toString(2));
            System.out.println(gje.getSolution().toString(2));
        } catch (MathException e) {
            System.err.println(e);
        }
    }
}