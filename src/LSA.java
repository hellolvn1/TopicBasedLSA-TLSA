
// [START all]
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.BigqueryScopes;

/**
 * Example of authorizing with Bigquery and reading from a public dataset.
 *
 * Specifically, this queries the shakespeare dataset to fetch the 10 of
 * Shakespeare's works with the greatest number of distinct words.
 */
public class LSA {
	// [START build_service]
	/**
	 * Creates an authorized Bigquery client service using Application Default
	 * Credentials.
	 *
	 * @return an authorized Bigquery client
	 * @throws IOException
	 *             if there's an error getting the default credentials.
	 */
	public static Bigquery createAuthorizedClient() throws IOException {
		// Create the credential
		HttpTransport transport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		GoogleCredential credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);

		// Depending on the environment that provides the default credentials
		// (e.g. Compute Engine, App
		// Engine), the credentials may require us to specify the scopes we need
		// explicitly.
		// Check for this case, and inject the Bigquery scope if required.
		if (credential.createScopedRequired()) {
			credential = credential.createScoped(BigqueryScopes.all());
		}

		return new Bigquery.Builder(transport, jsonFactory, credential).setApplicationName("Bigquery Samples").build();
	}
	// [END build_service]


	public static void main(String[] args) throws IOException, InterruptedException {
		

		String testFile = args[0];
		String outFile = args[1];
		String topicFile = args[2];
		Double threshold = Double.valueOf(args[3]);
		Integer dimension = Integer.valueOf(args[4]);
		Integer singular = 10;
		File file = new File(testFile);
		//File fileOut = new File(outFile);
		//FileUtils.deleteQuietly(fileOut);
		List<String> lsRecords = FileUtils.readLines(file);
		List<TweetRecord> lsTweets = new ArrayList<TweetRecord>();
		
		for (String rec : lsRecords) {
			String[] records = rec.split("\t");
			String topic_id = records[1];
			String topic = records[2].toLowerCase();
			String text1 = records[3].toLowerCase();
			String text2 = records[4].toLowerCase();
			Integer id = Integer.valueOf(records[0]);
			lsTweets.add(new TweetRecord(id,rec.toLowerCase(),text1,text2,topic_id,topic));
		}
		
		Map<String,List<TweetRecord>> mapTweets = lsTweets.stream().collect(Collectors.groupingBy(TweetRecord::getTopic)); 		
			for (Map.Entry<String, List<TweetRecord>> entry: mapTweets.entrySet()){
				System.out.println("Processing topic:" + entry.getKey());
				LSAThread R1 = new LSAThread(entry, outFile, topicFile, threshold,dimension,singular);
			    R1.start();
			}		
	}

	
	public static long countNumberOfOccurrences(String msg, final String target) {
	    return Arrays.stream(msg.split("[ ,\\.]")).filter(s -> s.equals(target)).count();
	}

}
