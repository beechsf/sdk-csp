package net.respectnetwork.sdk.csp.example;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.respectnetwork.sdk.csp.BasicCSP;
import net.respectnetwork.sdk.csp.CSPInformation;
import net.respectnetwork.sdk.csp.exception.CSPValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.client.XDIClient;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.http.XDIHttpClient;
import xdi2.core.Graph;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.xri3.CloudName;
import xdi2.core.xri3.CloudNumber;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;

public class ExampleUserGraphAccess {

	/* CHOOSE THE INDIVIDUAL's CLOUD NAME HERE */
	private static CloudName cloudName = CloudName.create("=test.beech5");
	
/* CHOOSE THE INDIVIDUAL's CLOUD Number HERE */
    private static CloudNumber cloudNumber = CloudNumber.create("[=]!:uuid:61866239-25bf-46e5-8917-648b71205ce8");

	/* CHOOSE THE INDIVIDUAL's SECRET TOKEN HERE */
	private static String secretToken = "qwerty123";
	
    /** Class Logger */
    private static final Logger logger = LoggerFactory
            .getLogger(ExampleUserGraphAccess.class);
    
	
	private static CSPInformation cspInformation;

	public static void main(String[] args) throws Xdi2ClientException {

		// Step 0: Set up CSP

		cspInformation = new CSPInformationRespectNetwork();
		//CSPInformation cspInformation = new CSPInformationTTCC();

		BasicCSP csp = new BasicCSP(cspInformation);

		// Step 1: Look for the Cloud Name's Cloud Number
		// If we already know the Cloud Number, then this step can be omitted.

		//csp.genericSetter(cloudName, cloudNumber, secretToken);
		try{
		  
		   genericGetXDICall(cloudName, cloudNumber, secretToken);
          //csp.genericSetXDICall(cloudName, cloudNumber, secretToken);
		  
	      Map<XDI3Segment, String> services = new HashMap<XDI3Segment, String> ();
	      services.put(XDI3Segment.create("<$https><$connect><$xdi>"), "http://respectconnect-dev.respectnetwork.net/respectconnect/");
          csp.setCloudServicesInCloud(cloudNumber, secretToken, services);
                  
	      System.out.println("Done setting service data for Cloud Name " + cloudName);

		} catch ( CSPValidationException e) {
		    e.printStackTrace();
		}
		// done

	}
	

	/**
	 * Test GET Method
	 * 
	 * @param cloudName
	 * @param cloudNumber
	 * @param secretToken
	 * @throws CSPValidationException
	 */
    public static void genericGetXDICall(CloudName cloudName, CloudNumber cloudNumber, String secretToken)
            throws CSPValidationException {
            
        try {
                
        MessageEnvelope messageEnvelope = new MessageEnvelope();
        //Sender
        //Message message = messageEnvelope.createMessage(this
               // .getCspInformation().getCspCloudNumber().getXri());
        Message message = messageEnvelope.createMessage(cloudNumber.getXri());
        //TO
        message.setToPeerRootXri(cloudNumber.getPeerRootXri());
        //message.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(secretToken);
        
             
        //Get
        
        XDI3Segment targetAddress = XDI3Segment.create(cloudNumber.toString());
        message.createGetOperation(targetAddress);
                
        
        // send message
        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                cspInformation, cloudNumber);

        XDIClient xdiClientCloud = new XDIHttpClient(cloudXdiEndpoint);

        MessageResult messageResult =xdiClientCloud.send(message.getMessageEnvelope(), null);
        
        if (messageResult.isEmpty()) {
            System.out.println("EMPTY!!!");
        }
        Graph theGraph = messageResult.getGraph();
        System.out.println (theGraph.getDeepLiteral(XDI3Segment.create(cloudNumber.toString() + "<+email>&")).getLiteralData());
        

        // done
        logger.debug("Completed Generic XDI OPERATION");

        } catch (Xdi2ClientException e) {
            logger.warn("Problem Creating User Graph Data", e.getMessage());
            throw new CSPValidationException(e.getMessage());
        }
    }
    
    /**
     * Test Graph SET Method
     */
    public void genericSetXDICall(CloudName cloudName, CloudNumber cloudNumber, String secretToken)
            throws CSPValidationException {
            
        try {
                
            MessageEnvelope messageEnvelope = new MessageEnvelope();
            //Sender
            Message message = messageEnvelope.createMessage(cloudNumber.getXri());
            //TO
            message.setToPeerRootXri(cloudNumber.getPeerRootXri());
            message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
            message.setSecretToken(secretToken);
               

            // Create

            String rc = "http://respectconnect-dev.respectnetwork.net/respectconnect/";
            
            XDI3Statement[] targetStatementsSet = new XDI3Statement[1];
            targetStatementsSet[0] = XDI3Statement.create("<$https><$connect><$xdi>&/&/\"" + rc  + "\"");
            
       
            message.createSetOperation(Arrays.asList(targetStatementsSet)
                    .iterator());
            
            // send message
    
            String cloudXdiEndpoint = makeCloudXdiEndpoint(
                    cspInformation, cloudNumber);
    
            XDIClient xdiClientCloud = new XDIHttpClient(cloudXdiEndpoint);
    
            xdiClientCloud.send(message.getMessageEnvelope(), null);
    
            // done
    
            logger.debug("Set value in graph.");
    

            } catch (Xdi2ClientException e) {
                logger.warn("Problem Creating User Graph Data", e.getMessage());
                throw new CSPValidationException(e.getMessage());
            }
    }
    
    /**
     * Utility  Method
     * 
     * @param cspInformation
     * @param cloudNumber
     * @return
     */
    private static String makeCloudXdiEndpoint(CSPInformation cspInformation,
            CloudNumber cloudNumber) {

        try {

            return cspInformation.getCspCloudBaseXdiEndpoint()
                    + URLEncoder.encode(cloudNumber.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
