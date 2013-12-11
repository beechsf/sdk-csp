package net.respectnetwork.sdk.csp;

import java.util.Map;

import net.respectnetwork.sdk.csp.exception.CSPRegistrationException;
import net.respectnetwork.sdk.csp.exception.CSPValidationException;
import net.respectnetwork.sdk.csp.model.CSPUser;
import net.respectnetwork.sdk.csp.model.CSPUserCredential;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.core.xri3.CloudName;
import xdi2.core.xri3.CloudNumber;
import xdi2.core.xri3.XDI3Segment;

/**
 * This interface represents CSP-related functionality of this SDK to communicate with
 * - the Respect Network Registration Service (RN)
 * - a CSP Environment (CSP)
 */
public interface CSP {
  
    /**
     *  2.1.1.2
     */
	public void registerCloudInCSP(CloudNumber cloudNumber, String secretToken)
	    throws Xdi2ClientException;
	
	/**
	 *  Pre 5.1.1.3  or 5.1.1.1
	 */
	public CloudNumber checkCloudNameAvailableInRN(CloudName cloudName)
	    throws Xdi2ClientException;

	/**
	 *  Part of 5.1.1.3.1
	 */ 
	public CloudNumber registerCloudNameInRN(CloudName cloudName)
	    throws Xdi2ClientException;
	
	/**
	 *  5.1.1.3  ( Member Graph )
	 */
	public void registerCloudNameInRN(CloudName cloudName, CloudNumber cloudNumber)
	    throws Xdi2ClientException;
	
	/**
	 *  5.1.1.7
	 */
	public void registerCloudNameInCSP(CloudName cloudName, CloudNumber cloudNumber)
	    throws Xdi2ClientException;
	
	/**
	 *  5.1.1.5 ( User Graph )
	 */
	public void registerCloudNameInCloud(CloudName cloudName, CloudNumber cloudNumber, String secretToken)
	    throws Xdi2ClientException;
	
	/** 3.1.1.5.1.1  Check if Phone ane Email are Unique*/
	public boolean checkVerifiedContactInformationInRN(String email, String phone)
	    throws Xdi2ClientException;
	
	/*  */
	public void setVerifiedContactInformationInRN(CloudNumber cloudNumber, String email, String phone)
	    throws Xdi2ClientException;

    /**
     *  Part of 5.1.1.3.1
     */ 
	public void setCloudXdiEndpointInRN(CloudNumber cloudNumber, String cloudXdiEndpoint)
	    throws Xdi2ClientException;
	
	/**
	 * ????
	 */
	public void setCloudXdiEndpointInCSP(CloudNumber cloudNumber, String cloudXdiEndpoint)
	    throws Xdi2ClientException;
	
	/**
	 *  5.1.1.1
	 */
	public void setCloudSecretTokenInCSP(CloudNumber cloudNumber, String secretToken)
	    throws Xdi2ClientException;
	
	/**
	 *  2.1.1.4.1
	 */
	public void setCloudServicesInCloud(CloudNumber cloudNumber, String secretToken, Map<XDI3Segment, String> services)
	    throws Xdi2ClientException;

   // Consolidate all of this in one API Call
    /**
     * Consolidates 5.1.1
     * 
     * The Secret Token will be stored in the CSP's Graph.
     * 
     * @param cloudNumber
     * @param cloudName
     * @param secretToken
     * @throws Xdi2ClientException
     */
    public void registerUserCloud(CloudNumber cloudNumber, CloudName cloudName, String secretToken)
            throws Xdi2ClientException;  
    
    
    /**
     * 
     * Consolidation of 2.1.1.2 and 2.1.1.4
     * 
     * Create a New User. Bootstrap the CSP and User Graph.
     * Seed the CSP  graph with a temporary  Secret Token
     * 
     * @param cloudNumber
     * @param xtraData
     * @throws Xdi2ClientException
     */
    public CSPUserCredential signUpNewUser()
            throws CSPRegistrationException; 
    
       
    /**
     * Validate User Information. (3.1.1)
     * 
     * 1) Send out Verification Contacts: SMS + eMail
     *     i) MetaData In XDI that details validation pattern. w/ Signatures
     * 2) Update the data in the CSP's User Graph. Mark as UnValidated.
     * 3) Check if Data qualifies for free account. ( i.e. neither email nor email are in MG Service ) 
     * 4) Fail and clean up graph.
     *  
     */
    public void createAndValidateUser(CloudNumber cloudNumber, CSPUser theUser, String secretToken)
            throws CSPValidationException; 
    
    /**
     * Validate Codes for both email and SMS. ( 4.1.1 )
     *
     */
    public boolean validateCodes(CloudNumber cloudNumber, String emailCode, String smsCode)
            throws CSPValidationException;
    
    /** 
     * Get  CSP  Info
     */    
    public CSPInformation getCspInformation();
    

}
