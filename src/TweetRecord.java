import org.apache.commons.math3.linear.RealMatrix;

public class TweetRecord {
	private Integer id;
	private String text1;
	private String text2;
	private String topic_id;
	private String topic;
	private RealMatrix mat1;
	private RealMatrix mat2;
	private Double score; 
	private String record;
	
	public String getRecord() {
		return record;
	}

	public void setRecord(String record) {
		this.record = record;
	}

	public TweetRecord(Integer aId, String rec, String aText1, String aText2, String aTopicID,String aTopic){
		id = aId;
		text1 = aText1;
		text2 = aText2;
		topic = aTopic;
		topic_id = aTopicID;
		record = rec;
	}
	
	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getText1() {
		return text1;
	}
	public void setText1(String text1) {
		this.text1 = text1;
	}
	public String getText2() {
		return text2;
	}
	public void setText2(String text2) {
		this.text2 = text2;
	}
	public String getTopic_id() {
		return topic_id;
	}
	public void setTopic_id(String topic_id) {
		this.topic_id = topic_id;
	}
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public RealMatrix getMat1() {
		return mat1;
	}
	public void setMat1(RealMatrix mat1) {
		this.mat1 = mat1;
	}
	public RealMatrix getMat2() {
		return mat2;
	}
	public void setMat2(RealMatrix mat2) {
		this.mat2 = mat2;
	}
	
	
}
