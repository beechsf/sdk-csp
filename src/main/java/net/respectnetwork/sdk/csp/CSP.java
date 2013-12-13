package net.respectnetwork.sdk.csp;

import java.util.Map;

import net.respectnetwork.sdk.csp.exception.CSPRegistrationException;
import net.respectnetwork.sdk.csp.exception.CSPValidationException;
import net.respectnetwork.sdk.csp.model.UserProfile;
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
	
	/** 3.1.1.5.1.1  Check if Phone and Email are Unique*/
	public boolean checkVerifiedContactInformationInRN(String email, String phone)
	    throws Xdi2ClientException;
	
	/**  This should be done as part of 3.1.1.5.1.1 but the MG Entry is not started until 5.1.1.3.1.3  */
	public void setVerifiedContactInformationInRN(CloudNumber cloudNumber, String email, String phone)
	    throws Xdi2ClientException;

    /**
     *  Part of 5.1.1.3.1
     */ 
	public void setCloudXdiEndpointInRN(CloudNumber cloudNumber, String cloudXdiEndpoint)
	    throws Xdi2ClientException;
	
	/**
	 * 
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

    /**
     * Finish the Registration Process 
     * 
     * 1) Write the  user provided secret token to the CSP Graph
     * 2) Register the ClouldName via EPP
     * 3) Update the RN  Member Graph
     * 4).Update CSP  Graph and Cloud Graph with Cloud Name
     * Consolidates 5.1.1
     * 
     * @param cloudNumber CloudNumber Previously created and provisioned in CSP and Cloud graphs.
     * @param cloudName User provided Cloud Name
     * @param secretToken User Provided Secret Token
     * @throws Xdi2ClientException
     */
    public void registerUserCloud(CloudNumber cloudNumber, CloudName cloudName, String secretToken)
            throws Xdi2ClientException;  
    
    
   /**
    * Begin the Registration process for a new User
    * Create a New User with a generated CloudNumber.
    * Bootstrap the CSP and Cloud Graph.
    * Seed the CSP graph with a temporary Secret Token
    * Consolidation of 2.1.1.2 and 2.1.1.4
    * 
    * @return CSPUserCredential containing cloud number and temporary secret token
    * @throws CSPRegistrationException
    */
    public CSPUserCredential signUpNewUser()
            throws CSPRegistrationException; 
    
       
    /**
     * Create User Graph and Validate User Information. (3.1.1)
     * 
     * 1) Send out Verification Contacts: SMS + eMail
     * 2) Update the data in the CSP's User Graph. Mark as UnValidated.
     * 3) Check if Data qualifies for free account. ( i.e. neither email nor email are in MG Service ) 
     *  
     *
     * @param cloudNumber 
     * @param theUser User Personal Data 
     * @param secretToken 
     * @throws CSPValidationException
     */
    public void setUpAndValidateUserProfileInCloud(CloudNumber cloudNumber, UserProfile theUser, String secretToken)
            throws CSPValidationException; 
    
    /**
     * Validate Codes for both email and SMS as part of the registration process
     * and update <+email> and <+phone> in the UserGraph with
     * 
     * <+validation>/+validator/[@]!:uuid:9999
     * <+validation><+validationdate>&/&/ = "10:10:2013"
     * <+validation><+validationsignature>&/&/ = "sig(emailaddress)"  ( Signed by CSP/Validator ) 
     *      
     * @param cloudNumber Cloud Number created for new user. Key for Lookup.
     * @param emailCode validationCode e-mailed to User
     * @param smsCode validation code SMSed to  user
     * @param secretToken required to  update user's Graph
     * @throws CSPValidationException
     * 
     */
    public boolean validateCodes(CloudNumber cloudNumber, String emailCode, String smsCode, String secretToken)
            throws CSPValidationException;
    
    /** 
     * Get  CSP Information
     */    
    public CSPInformation getCspInformation();
    

}
