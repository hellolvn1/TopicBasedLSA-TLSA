import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;

public class LSAThread implements Runnable{
	private Map.Entry<String, List<TweetRecord>> entry;
	private Thread t;
	private String outputFolder;
	private String topicFolder;
	private Double threshold;
	private Integer dimension;
	private Integer singular;

	public LSAThread(Map.Entry<String, List<TweetRecord>> aEntry, String out, String topic, double value, Integer dim, Integer sing) {
		this.entry = aEntry;
		this.outputFolder = out;
		this.topicFolder = topic;
		this.threshold = value;
		this.dimension = dim;
		this.singular = sing;
	}

	
	public  RealMatrix getRanked(RealMatrix matrix, int rank, char delim) {

		double[][] doubleMat = null;

		switch (delim) {

		case 'S':
			doubleMat=new double[rank][rank];
			for (int i = 0; i < rank; i++) {
				double[] rowMatrix = matrix.getRow(i);

				for (int j = 0; j < rank; j++) {
					doubleMat[i][j] = rowMatrix[j];
				}

			}
			break;
		default: {
			doubleMat=new double[matrix.getRowDimension()][rank];
			for (int i = 0; i < matrix.getRowDimension(); i++) {
				
				double[] rowMatrix = matrix.getRow(i);
				for (int j = 0; j < rank; j++) {
					
					doubleMat[i][j] = rowMatrix[j];
				}

			}
		}
		}

		RealMatrix mat = MatrixUtils.createRealMatrix(doubleMat);
		return mat;

	}
	
	@Override
	public void run() {

		String topicId = entry.getValue().get(0).getTopic_id();

		List<String> lsGram1 = new ArrayList<>();
		Map<String, Integer> mapGram5 = new HashMap<>();

		for (TweetRecord t : entry.getValue()) {
			for (String token : t.getText1().split(" ")) {
				if (!lsGram1.contains(token.trim())) {
					lsGram1.add(token);
				}
			}
			for (String token : t.getText2().split(" ")) {
				if (!lsGram1.contains(token.trim())) {
					lsGram1.add(token);
				}
			}
		}
		
		//load 5 gram
		List<File> files = new ArrayList<>();
		files = (List<File>) FileUtils.listFiles(new File(topicFolder), new WildcardFileFilter(topicId + "*"), TrueFileFilter.TRUE);
		outerloop:
		for (File file: files){
			try {	
				List<String> lsRecords = FileUtils.readLines(file); 
				lsRecords.remove(0);
				for (String line: lsRecords ){
					mapGram5.put(line.split(",")[0], Integer.valueOf(line.split(",")[1]));
					if (mapGram5.size() >= dimension){
						break outerloop;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//
		// construct matrix
		System.out.println("Constructing matrix:" + topicId);
		double[][] array = new double[lsGram1.size()][mapGram5.size()];
		for (int i = 0; i < lsGram1.size(); i++) {
			String tword = lsGram1.get(i);
			int j = 0;
			for (Map.Entry<String, Integer> gram5 : mapGram5.entrySet()) {
				if (gram5.getKey().contains(tword)) {
					array[i][j++] = Double.valueOf(gram5.getValue());
				} else {
					array[i][j++] = 0;
				}

			}
		}
//		System.out.println("Finish constructing matrix:" + topicId);
//
//		// SVD decomposion
//		// use Efficient Java Matrix Library (EJML) for SVD
//		// (Jama and Apache Commons Math do not handle matrices with m<n correctly)
//		SimpleMatrix matA = new SimpleMatrix(array);
//		SimpleSVD<SimpleMatrix> svd = matA.svd();
//		SimpleMatrix U = svd.getU();
//		SimpleMatrix S = svd.getW().invert();
		
		RealMatrix matrix = MatrixUtils.createRealMatrix(array);

		SingularValueDecomposition svd = new SingularValueDecomposition(
				matrix);
		
		double[] singulars = svd.getSingularValues();
		
		int max = singulars.length-1;
		
		for (int k=singulars.length-1;k>=0;k--){
			if (singulars[k] == 0.0){
				max--;
			}else{
				break;
			}
		}
		for (int count=75;count<=85;count++){
			if (count == 80){
				continue;
			}
			int temp = count;
			
			if (count > max){
				temp = max;
			}

			RealMatrix S=getRanked(svd.getS(), temp, 'S');
			
			//inverse S
			RealMatrix S_inverse = null;
			try{
				S_inverse = new LUDecomposition(S, 0.0).getSolver().getInverse();
			}catch(Exception ex){
				System.out.println(ex.getMessage());
				return;
			}
			
			
			RealMatrix U=getRanked(svd.getU(), temp, 'U');

			//
			// calculate tweet1 and tweet2 vectors
			for (TweetRecord t : entry.getValue()) {
				String text1 = t.getText1();
				String text2 = t.getText2();
				double[][] t1 = new double[1][lsGram1.size()];
				double[][] t2 = new double[1][lsGram1.size()];

				for (int i = 0; i < lsGram1.size(); i++) {
					String uni = lsGram1.get(i);
					long count1 = countNumberOfOccurrences(text1, uni);
					long count2 = countNumberOfOccurrences(text2, uni);
					t1[0][i] = count1;
					t2[0][i] = count2;
				}

				// calculate vector t1,mt2
				RealMatrix q1 = MatrixUtils.createRealMatrix(t1);
				RealMatrix q2 = MatrixUtils.createRealMatrix(t2);
				

				RealMatrix result1 = q1.multiply(U).multiply(S_inverse);
				RealMatrix result2 = q2.multiply(U).multiply(S_inverse);
				//DenseMatrix result1 = q1.multiply(U_matrix).multiply((DenseMatrix) sm_inverse);
				//DenseMatrix result2 = q2.multiply(U_matrix).multiply((DenseMatrix) sm_inverse);

				double[] arr1 = result1.getData()[0];
				double[] arr2 = result2.getData()[0];
				DoubleMatrix1D a = new DenseDoubleMatrix1D(arr1);
				DoubleMatrix1D b = new DenseDoubleMatrix1D(arr2);
				double cosineDistance = Math.abs(a.zDotProduct(b) / Math.sqrt(a.zDotProduct(a) * b.zDotProduct(b)));
				DecimalFormat df = new DecimalFormat("0.0000");
				df.setRoundingMode(RoundingMode.CEILING);

				File fileOut = new File(outputFolder + this.dimension + "_" + count + File.separator + t.getId() + ".txt");
				FileUtils.deleteQuietly(fileOut);

				if (cosineDistance >= threshold) {
					try {
						FileUtils.writeStringToFile(fileOut,
								"true" + "\t" + df.format(cosineDistance) + System.getProperty("line.separator"), true);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						FileUtils.writeStringToFile(fileOut,
								"false" + "\t" + df.format(cosineDistance) + System.getProperty("line.separator"), true);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println(cosineDistance);
			}
		}
		
		System.out.print("Finish:" + topicId);
	}
	
	public static long countNumberOfOccurrences(String msg, final String target) {
		return Arrays.stream(msg.split("[ ,\\.]")).filter(s -> s.equals(target)).count();
	}

	public void start() {
		if (t == null) {
			t = new Thread(this, entry.getKey());
			t.start();
		}
	}
}
