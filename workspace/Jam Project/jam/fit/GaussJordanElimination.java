package jam.fit;

class GaussJordanElimination {

	private Matrix InputMatrix; // n by n
	private Matrix InputVectors; // n by m

	private int[] RowIndex;
	private int[] ColumnIndex;
	private int[] Pivot;

	private int n;
	private int m;

	private int PivotRow;
	private int PivotColumn;

	GaussJordanElimination(Matrix InputMatrix, Matrix InputVector) {

		this.InputMatrix = new Matrix(InputMatrix);
		this.InputVectors = new Matrix(InputVector);

		this.n = InputMatrix.rows;
		this.m = InputVectors.columns;

		if (n != InputVectors.rows) {
			throw new IllegalArgumentException(
				n + " not equal to " + InputVectors.rows + "!!!");
		}

		RowIndex = new int[n];
		ColumnIndex = new int[n];
		Pivot = new int[n];

	}

	public void go() throws Exception {
		clearPivot();
		findPivots();
	}

	private void clearPivot() {
		int i;

		for (i = 0; i < n; i++) {
			Pivot[i] = 0;
		}
	}

	private void findPivots() throws Exception {
		int k;
		int j;
		int ll;
		int l;
		int i;
		double big;
		double PivotInverse;
		double dummy;

		for (i = 0; i < n; i++) {
			big = 0.0;
			for (j = 0; j < n; j++) {
				if (Pivot[j] != 1) {
					for (k = 0; k < n; k++) {
						if (Pivot[k] == 0) {
							if (Math.abs(InputMatrix.element[j][k]) >= big) {
								big = Math.abs(InputMatrix.element[j][k]);
								PivotRow = j;
								PivotColumn = k;
							}
						} else if (Pivot[k] > 1) {
							throw new Exception("GJE: Singular Matrix-1");
						}
					}
				}
			}
			Pivot[PivotColumn]++;
			if (PivotRow != PivotColumn) { //put pivot element on diagonal
				InputMatrix.permute(PivotRow, PivotColumn, 'r');
				InputVectors.permute(PivotRow, PivotColumn, 'r');
			}
			RowIndex[i] = PivotRow;
			ColumnIndex[i] = PivotColumn;
			if (InputMatrix.element[PivotColumn][PivotColumn] == 0.0) {
				throw new Exception("GJE: Singular Matrix-2");
			}
			PivotInverse = 1.0 / InputMatrix.element[PivotColumn][PivotColumn];
			InputMatrix.element[PivotColumn][PivotColumn] = 1.0;
			InputMatrix.rowMultiply(PivotColumn, PivotInverse);
			InputVectors.rowMultiply(PivotColumn, PivotInverse);
			for (ll = 0; ll < n; ll++) {
				if (ll != PivotColumn) {
					dummy = InputMatrix.element[ll][PivotColumn];
					InputMatrix.element[ll][PivotColumn] = 0.0;
					for (l = 0; l < n; l++) {
						InputMatrix.element[ll][l] =
							InputMatrix.element[ll][l]
								- InputMatrix.element[PivotColumn][l] * dummy;
					}
					for (l = 0; l < m; l++) {
						InputVectors.element[ll][l] =
							InputVectors.element[ll][l]
								- InputVectors.element[PivotColumn][l] * dummy;
					}
				}
			}
		}
		//Now unscramble the permuted columns.
		for (l = n - 1; l >= 0; l--) {
			if (RowIndex[l] != ColumnIndex[l]) {
				InputMatrix.permute(RowIndex[l], ColumnIndex[l], 'r');
			}
		}
	}

	public Matrix getMatrix() {
		return InputMatrix;
	}
	public Matrix getVectors() {
		return InputVectors;
	}
}