package net.respectnetwork.sdk.csp;

import java.util.Map;

import net.respectnetwork.sdk.csp.exception.CSPRegistrationException;
import net.respectnetwork.sdk.csp.exception.CSPValidationException;
import net.respectnetwork.sdk.csp.model.CSPUserCredential;
import net.respectnetwork.sdk.csp.model.UserProfile;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.core.xri3.CloudName;
import xdi2.core.xri3.CloudNumber;
import xdi2.core.xri3.XDI3Segment;

/**
 * This interface represents CSP-related functionality of this SDK to communicate with
 * - the Respect Network Registration Service (RN)
 * - a CSP Environment (CSP)
 * - a User Cloud
 * 
 * The comments on individual methods reference the following diagrams:
 * [A] Initial Sign-Up: https://wiki.respectnetwork.net/wiki/Alice_Signs_Up
 * [B] Inline Provisioning: https://docs.google.com/a/respectnetwork.net/file/d/0B76sKV7mFhHNeEkyN3JTOGVabEk/
 */
public interface CSP {
  
	/*
	 * Methods for registering User Clouds
	 */

	/**
	 * This method registers a new User Cloud in the CSP Cloud.
	 * In addition:
	 *   - this registers the User Cloud's XDI endpoint in the CSP Cloud.
	 *   - this optionally registers the User Cloud's secret token in the CSP Cloud.
	 * Used in:
	 *   [A] Step 2.1.1.2 
	 *   [B] Step 5.1.2
	 */
	public void registerCloudInCSP(CloudNumber cloudNumber, String secretToken) throws Xdi2ClientException;

	/*
	 * Methods for registering Cloud Names
	 */

	/**
	 * This method checks if a Cloud Name has been registered in the RN Registration Service.
	 * @return A Cloud Number if the Cloud Name has been registered, otherwise null.
	 * Used in:
	 *   [A] Not used
	 *   [B] Not used
	 */
	public CloudNumber checkCloudNameAvailableInRN(CloudName cloudName) throws Xdi2ClientException;

	/**
	 * This method checks if a verified phone number and verified e-mail address have been registered in the RN Member Graph Service.
	 * @return An array of exactly two Cloud Numbers that have been registered for the phone number and the e-mail address respectively.
	 * Each one of the two Cloud Numbers may be null, if the phone number or e-mail address have not been registered.
	 * Used in:
	 *   [A] 3.1.1.5.1
	 *   [B] Not used
	 */
	public CloudNumber[] checkPhoneAndEmailAvailableInRN(String verifiedPhone, String verifiedEmail) throws Xdi2ClientException;

	/**
	 * This method registers a new Cloud Name and Cloud Number in the RN Registration Service.
	 * In addition:
	 *   - this registers the Cloud Name and Cloud Number in the RN Member Graph Service.
	 *   - this registers the User Cloud's XDI endpoint in the RN Registration Service.
	 *   - this optionally registers a verified phone number and verified e-mail address in the RN Member Graph Service.
	 * Used in:
	 *   [A] 5.1.1.3
	 *   [B] Not used
	 */
	public void registerCloudNameInRN(CloudName cloudName, CloudNumber cloudNumber, String verifiedPhone, String verifiedEmail) throws Xdi2ClientException;

	/**
	 * This method registers a new Cloud Name and Cloud Number in the CSP Cloud.
	 * Used in:
	 *   [A] 5.1.1.7
	 *   [B] Not used
	 */
	public void registerCloudNameInCSP(CloudName cloudName, CloudNumber cloudNumber) throws Xdi2ClientException;

	/**
	 * This method registers a new Cloud Name and Cloud Number in the User Cloud.
	 * Used in:
	 *   [A] 5.1.1.5
	 *   [B] Not used
	 */
	public void registerCloudNameInCloud(CloudName cloudName, CloudNumber cloudNumber, String secretToken) throws Xdi2ClientException;

	/*
	 * Methods for updating User Clouds or Cloud Names after they have been registered  
	 */

	/**
	 * This method updates a User Cloud's XDI endpoint in the RN Registration Service.
	 * Normally, it is not necessary to call this, since it is automatically done by the registerCloudNameInRN() method.
	 * Used in:
	 *   [A] Not used
	 *   [B] 5.1.2
	 */
	public void setCloudXdiEndpointInRN(CloudNumber cloudNumber, String cloudXdiEndpoint) throws Xdi2ClientException;

	/**
	 * This methods updates a verified phone number and verified e-mail address in the RN Member Graph Service.
	 * Normally, it is not necessary to call this, since it is automatically done by the registerCloudNameInRN() method.
	 * Used in:
	 *   [A] Not used
	 *   [B] 5.1.2
	 */
	public void setPhoneAndEmailInRN(CloudNumber cloudNumber, String verifiedPhone, String verifiedEmail) throws Xdi2ClientException;

	/**
	 * This method updates a User Cloud's XDI endpoint in the CSP Cloud.
	 * Normally, it is not necessary to call this, since it is automatically done by the registerCloudInCSP() method.
	 * Used in:
	 *   [A] Not used
	 *   [B] Not used
	 */
	public void setCloudXdiEndpointInCSP(CloudNumber cloudNumber, String cloudXdiEndpoint) throws Xdi2ClientException;

	/**
	 * This method updates a User Cloud's secret token in the CSP Cloud.
	 * Normally, it is not necessary to call this, since it is automatically done by the registerCloudInCSP() method.
	 * Used in:
	 *   [A] 5.1.1.1
	 *   [B] Not used
	 */
	public void setCloudSecretTokenInCSP(CloudNumber cloudNumber, String secretToken) throws Xdi2ClientException;

	/**
	 * This method updates additional services in a User Cloud.
	 * Used in:
	 *   [A] 2.1.1.4
	 *   [B] Not used
	 */
	public void setCloudServicesInCloud(CloudNumber cloudNumber, String secretToken, Map<XDI3Segment, String> services) throws Xdi2ClientException;

	/**
	 * This methods updates a verified phone number and verified e-mail address in the User's Cloud.
	 * Used in:
	 *   [A] 3.1.1.3
	 *   [B] Not used
	 */
	public void setPhoneAndEmailInCloud(CloudNumber cloudNumber, String verifiedPhone, String verifiedEmail) throws Xdi2ClientException;

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
