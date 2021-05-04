/**
 * @author Gerrit Brink
 */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class SimpleTCPConnectionWrapper{
	
	private static String  IP = "127.0.0.1";//or specify a dns
	private static Integer PORT = 9552;
	
	//XML tokens or messagess used as the tokens in this example
    private static String  TCP_MSG = "<foo client=\"XmlClient\" term=\"XmlTerm\" seqNum=\"0\" time=\"1970-01-01 00:00:00 +0000\">" +
						    		  "<Msg ver=\"1.0\">" +
							    		  "<vendReq>" +
								    		  "<ref>1234567890</ref>" +
								    		  "<amt cur=\"ZAR\">10000</amt>" +
								    		  "<numTokens>1</numTokens>" +
								    		  "<num></num>" +
							    		  "</vendReq>" +
						    		  "</Msg>" +
					    		  "</foo>";
    
	public static void main(String[] args){
		try{
			//Builder factories for XML to document
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			
			//Initialize the Tokens document
			Document tokenDocument = stringToDocument(builder, TCP_MSG);
			
			//Establish a connection to the server
			try(TCPConnection tcpConn = new TCPConnection(IP, PORT);
				Scanner keboardInput = new Scanner(System.in)){
				
				String ipt = null;
				while(!"x".equalsIgnoreCase(ipt)){
					if(ipt != null && !ipt.trim().isEmpty()){//Basic validation
						//Set new tokens meter number then send it to the server
						tokenDocument.getElementsByTagName("num").item(0).setTextContent(ipt);
						String serverResponse = tcpConn.sendMessage(documentToString(transformer, tokenDocument));

						//Print token response
						Document responseDoc = stringToDocument(builder, serverResponse);
						String tmp = getFirstNodeText(responseDoc, "stdToken");
						if(tmp != null)
							System.out.printf("%s%n%n", tmp);
						
						tmp = getFirstNodeText(responseDoc, "polToken");
						if(tmp != null)
							System.out.printf("%s%n%n", tmp);

						tmp = getFirstNodeText(responseDoc, "res");
						if(!"OK".equals(tmp))//If an error was returned, then print it
							System.out.printf("%s%n%n", tmp);
						
					}
					
					//Capture keyboard input
					System.out.print("Please supply a token or type 'x' to quit:");
					ipt = keboardInput.nextLine();
				}
			}
		}catch(Exception e){//Catch all exceptions for now, further refinement per exception can be implemented
			e.printStackTrace();
		}
	}
	
	/**
	 * @param doc
	 * @param nodeName
	 * @return String contents of the Node, can return null
	 */
	private static String getFirstNodeText(Document document, String nodeName){
		Node n = document.getElementsByTagName(nodeName).item(0);
		return n == null ? null : n.getTextContent();
	}
	
	/**
	 * @param transformer
	 * @param document
	 * @return String value of the XML Document
	 * @throws TransformerException
	 */
	private static String documentToString(Transformer transformer, Document document) throws TransformerException{
		DOMSource domSource = new DOMSource(document);
		StringWriter sw = new StringWriter();
		StreamResult streamResult = new StreamResult(sw);
		transformer.transform(domSource, streamResult);
		return sw.toString();
	}
	
	/**
	 * @param builder
	 * @param xml
	 * @return XML Document Object
	 * @throws SAXException
	 * @throws IOException
	 */
	private static Document stringToDocument(DocumentBuilder builder, String xml) throws SAXException, IOException{
		return builder.parse(new ByteArrayInputStream(xml.getBytes()));
	}
}


/**
 * @author Gerrit Brink
 * TCPConnection class would typically go into it's own file, for convenience I put it here
 */
class TCPConnection implements AutoCloseable{
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	
	/**
	 * Opens a TCP connection to the server
	 * @param ip
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public TCPConnection(String ip, Integer port) throws UnknownHostException, IOException{
		socket = new Socket(ip, port);
		out = new DataOutputStream(socket.getOutputStream());
		in = new DataInputStream(socket.getInputStream());
	}
	
	/**
	 * Sends a string message to the server
	 * @param String message to be sent to the server
	 * @return String response from server
	 */
	public String sendMessage(String message) throws IOException{
		//Send message to server      
		out.writeShort(message.length());
		out.write(message.getBytes());

		//Read server response
		short messageSize = in.readShort();
		byte[] buffer = new byte[messageSize];
		in.readFully(buffer, 0, messageSize);
		return new String(buffer);
	}
	
	/**
	 * Close the TCP connection quietly
	 */
	@Override
	public void close(){
		try {in.close();} catch (IOException e){}
		try {out.close();} catch (IOException e){}
		try {socket.close();} catch (IOException e){}
	}
}
