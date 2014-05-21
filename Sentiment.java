
import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Sentiment {

	static private HashSet<String> Negative, Positive; //������дʵ�
	static private Integer NegativeDoc, PositiveDoc, UnsureDoc; 	//����������е��ı��� 	- ������ģ����Ҫ�����µ�ֵ
	static private Hashtable<String, Integer> NegativeWeight, PositiveWeight, UnsureWeight; 	//������������д�������Ȩֵ - ������ģ����Ҫ�����µ�ֵ
	
	public static void main(String[] args) throws Exception {
		// TODO �Զ����ɵķ������
		Sentiment Sentiment = new Sentiment();
		
		Sentiment.Model( );
		Sentiment.Save_Model();
	}
	
	public void Model( ) throws Exception {
		
		this.Read_Sentiment_Dictionary();
		this.Sentiment_Doc_Weight("500trainblogxml - ����/");
	
	}
	
	@SuppressWarnings("resource")
	public void Read_Sentiment_Dictionary() throws Exception {
		
		BufferedReader buf;
		String str;
		
		Negative = new HashSet<String>();
		buf = new BufferedReader( new InputStreamReader(new FileInputStream("PMIstock-dictionary/negative.txt"), "UTF-8") );
		while( (str = buf.readLine()) != null ) {
			Negative.add(str);
		}
		
		Positive = new HashSet<String>();
		buf = new BufferedReader( new InputStreamReader(new FileInputStream("PMIstock-dictionary/positive.txt"), "UTF-8") );
		while( (str = buf.readLine()) != null ) {
			Positive.add(str);
		}
	}
	
	
	public void Sentiment_Doc_Weight( String DirPath ) throws Exception {
		
		File NegativeDir = new File( DirPath + "negativeout" );
		String[] NegativeFiles = NegativeDir.list();
		NegativeDoc = NegativeFiles.length;
		ArrayList<String> NegativeCurrentList = new ArrayList<String>();
		for ( int i = 0; i < NegativeFiles.length; i ++ ) {
			System.out.println("NegativeFiles No."+(i+1)+" "+DirPath+"negativeout/"+NegativeFiles[i]);
			this.ReadXML(DirPath+"negativeout/"+NegativeFiles[i], NegativeCurrentList);
		}
		NegativeWeight = HashTable( NegativeCurrentList );
		
		/**********************************************************************************************************/
		
		File PositiveDir = new File( DirPath + "positiveout" );
		String[] PositiveFiles = PositiveDir.list();
		PositiveDoc = PositiveFiles.length;
		ArrayList<String> PositiveCurrentList = new ArrayList<String>();
		for ( int i = 0; i < PositiveFiles.length; i ++ ) {
			System.out.println("PositiveFiles No."+(i+1)+" "+DirPath+"positiveout/"+PositiveFiles[i]);
			this.ReadXML(DirPath+"positiveout/"+PositiveFiles[i], PositiveCurrentList);
		}
		PositiveWeight = HashTable( PositiveCurrentList );
		
		/*********************************************************************************************************/
		
		File UnsureDir = new File( DirPath + "unsureout" );
		String[] UnsureFiles = UnsureDir.list();
		UnsureDoc = UnsureFiles.length;
		ArrayList<String> UnsureCurrentList = new ArrayList<String>();
		for ( int i = 0; i < UnsureFiles.length; i ++ ) {
			System.out.println("UnsureFiles No."+(i+1)+" "+DirPath+"unsureout/"+UnsureFiles[i]);
			this.ReadXML(DirPath+"unsureout/"+UnsureFiles[i], UnsureCurrentList);
		}
		UnsureWeight = HashTable( UnsureCurrentList );
		
		/********************************************************************************************************/
		System.out.println("UnsureCurrent = " + UnsureCurrentList.size() + "  UnsureHashTable = " + UnsureWeight.size());
		System.out.println("PositiveCurrent = " + PositiveCurrentList.size() + "  PositiveHashTable = " + PositiveWeight.size());
		System.out.println("NegativeCurrent = " + NegativeCurrentList.size() + "  NegativeHashTable = " + NegativeWeight.size());
		System.out.println("NegativeDoc = " + NegativeDoc +  "  PositiveDoc = " + PositiveDoc + "  UnsureDoc = " + UnsureDoc);
		
	}
	
	public void ReadXML( String FilePath, ArrayList<String> currentList ) throws Exception { 	//��ָ��·����ȡXML�ļ�����ȡ������дʷ���
		
		SAXReader SaxReader = new SAXReader();
		Document Doc = SaxReader.read(new File(FilePath));
		Element root = Doc.getRootElement();
		
		Element content = root.element("content");
		List<?> sentenses = content.elements("sentence");	 //ÿһ�仰��Ϊһ��
		
		for ( Iterator<?> iter = sentenses.iterator(); iter.hasNext();  ) {
			Element sentense = (Element)iter.next();
			
			List<?> toks = sentense.elements();
			for ( Iterator<?> iter1 = toks.iterator(); iter1.hasNext(); ) {
				Element tok = (Element)iter1.next();
				String Type = tok.attributeValue("type");
				
				if ( Type.equals("group") ) { 		//�����"atom"һ������������д���
					GetWord( tok, currentList ); 	//��"group"�л�ȡ��
				}
			}
		}
	}
	
	public void GetWord( Element root, ArrayList<String> currentList ) { 		//��ȡXML�е���д�
		
		String Word = "";
		List<?> elements = root.elements("tok");
		for ( Iterator<?> iter = elements.iterator(); iter.hasNext(); ) {
			Element tok = (Element)iter.next();
			String Type = tok.attributeValue("type");
			
			if ( Type.compareTo("atom") == 0 ) {
				Word += tok.getText().trim();
			}
			else {
				GetWord( tok, currentList );
			}
		}
		if ( Word.length() > 1 && (Positive.contains(Word) || Negative.contains(Word)) ) {  //ɸѡ����д�
			currentList.add(Word);
		}
	}
	
	public Hashtable<String, Integer> HashTable( ArrayList<String> currentList ) { 	//�����ı��е���дʹ�����ϣ��
		
		Hashtable<String, Integer> HashTable = new Hashtable<String, Integer>();
		
		for ( Iterator<String> iter = currentList.iterator(); iter.hasNext();  ) {
			String Word = (String)iter.next();
			if ( HashTable.containsKey(Word) ) {
				Integer Weight = HashTable.get(Word);
				HashTable.put(Word, Weight+1);
			}
			else {
				HashTable.put(Word, 1);
			}
		}
		return HashTable;
	}
	
	@SuppressWarnings("resource")
	public void Save_Model( ) throws Exception {
		
		ObjectOutputStream OOS;
		File ModelPath = new File("Model");
		File NegativeModel = new File(ModelPath, "NegativeModel.txt");
		File PositiveModel = new File(ModelPath, "PositiveModel.txt");
		File UnsureModel = new File(ModelPath, "UnsureModel.txt");
		
		if ( !ModelPath.exists() ) {	ModelPath.mkdir();	}
		
		System.out.println("Saving NegativeModel...");
		OOS = new ObjectOutputStream( new FileOutputStream( NegativeModel ) ); 	//������ֱ��д��
		OOS.writeObject(NegativeDoc);
		OOS.writeObject(NegativeWeight);
		
		System.out.println("Saving PositiveModel...");
		OOS = new ObjectOutputStream( new FileOutputStream( PositiveModel ) );
		OOS.writeObject(PositiveDoc);
		OOS.writeObject(PositiveWeight);
		
		System.out.println("Saving UnsureModel...");
		OOS = new ObjectOutputStream( new FileOutputStream( UnsureModel ) );
		OOS.writeObject(UnsureDoc);
		OOS.writeObject(UnsureWeight);
		
		Enumeration<String> Keys;
		System.out.println("Saving NegativeWeight...");
		Keys = NegativeWeight.keys();
		while( Keys.hasMoreElements() ) {
			String Key = Keys.nextElement();
			FileUtils.writeStringToFile(new File("Model", "NegativeWeight.txt"), Key+"\t\t\t"+NegativeWeight.get(Key)+"\r\n", "UTF-8", true);
		}
		System.out.println("Saving PositiveWeight...");
		Keys = PositiveWeight.keys();
		while( Keys.hasMoreElements() ) {
			String Key = Keys.nextElement();
			FileUtils.writeStringToFile(new File("Model", "PositiveWeight.txt"), Key+"\t\t\t"+PositiveWeight.get(Key)+"\r\n", "UTF-8", true);
		}
		System.out.println("Saving UnsureWeight...");
		Keys = UnsureWeight.keys();
		while( Keys.hasMoreElements() ) {
			String Key = Keys.nextElement();
			FileUtils.writeStringToFile(new File("Model", "UnsureWeight.txt"), Key+"\t\t\t"+UnsureWeight.get(Key)+"\r\n", "UTF-8", true);
		}
		
		System.out.println("Save Success!");
	}
}