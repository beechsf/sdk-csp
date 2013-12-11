package net.respectnetwork.sdk.csp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.respectnetwork.sdk.csp.exception.CSPRegistrationException;
import net.respectnetwork.sdk.csp.exception.CSPValidationException;
import net.respectnetwork.sdk.csp.exception.MessageCreationException;
import net.respectnetwork.sdk.csp.model.CSPUser;
import net.respectnetwork.sdk.csp.model.CSPUserCredential;
import net.respectnetwork.sdk.csp.notification.MessageManager;
import net.respectnetwork.sdk.csp.notification.NotificationException;
import net.respectnetwork.sdk.csp.notification.Notifier;
import net.respectnetwork.sdk.csp.notification.TokenException;
import net.respectnetwork.sdk.csp.notification.TokenKey;
import net.respectnetwork.sdk.csp.notification.TokenManager;
import net.respectnetwork.sdk.csp.xdi.CloudNumberGenerator;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.client.XDIClient;
import xdi2.client.constants.XDIClientConstants;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.http.XDIHttpClient;
import xdi2.core.Relation;
import xdi2.core.constants.XDIAuthenticationConstants;
import xdi2.core.constants.XDIConstants;
import xdi2.core.constants.XDIDictionaryConstants;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.features.nodetypes.XdiPeerRoot;
import xdi2.core.util.XDI3Util;
import xdi2.core.xri3.CloudName;
import xdi2.core.xri3.CloudNumber;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;


/**
 *  Basic Implementation of CSP SDK API.
 *
 */
public class BasicCSP implements CSP {

    /** Class Logger */
    private static final Logger log = LoggerFactory.getLogger(BasicCSP.class);

    /** REgistrar Link Contract */
    public static final XDI3Segment REGISTRAR_LINK_CONTRACT = XDI3Segment
            .create("+registrar$do");

    /** CSP  Information */
    private CSPInformation cspInformation;

    /** XDIClient for  RespectNetwork Registration Service*/
    private XDIClient xdiClientRNRegistrationService;
    
    /** XDIClient for CSP Registry */
    private XDIClient xdiClientCSPRegistry;

    /** CloudNumber Generator to Use */
    private CloudNumberGenerator cloudNumberGenerator;
    
    /** Notification Service */
    private Notifier theNotifier;
    
    /** Token Manager */
    private TokenManager tokenManager;
    
    /** Message Manager */
    private MessageManager messageManager;
    
    /** Registration Codes Validation Endpoint */
    private String validationEndpoint;
    
    /**
     *  Default Constructor
     */
    public BasicCSP() {
        // Do Nothing
    }

    
    /** 
     * Constructor  with CSPInformation
     * @param cspInformation
     */
    public BasicCSP(CSPInformation cspInformation) {

        this.cspInformation = cspInformation;

        this.xdiClientCSPRegistry = new XDIHttpClient(
                cspInformation.getCspRegistryXdiEndpoint());
        this.xdiClientRNRegistrationService = new XDIHttpClient(
                cspInformation.getRnRegistrationServiceXdiEndpoint());
    }



    
    /**
     * {@inheritDoc}
     */
    public void registerCloudInCSP(CloudNumber cloudNumber, String secretToken)
            throws Xdi2ClientException {

        // prepare message to CSP

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getCspCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                this.getCspInformation(), cloudNumber);

        XDI3Statement[] targetStatementsDoDigestSecretToken = new XDI3Statement[] { XDI3Statement
                .fromLiteralComponents(XDI3Util.concatXris(
                        cloudNumber.getPeerRootXri(),
                        XDIAuthenticationConstants.XRI_S_DIGEST_SECRET_TOKEN,
                        XDIConstants.XRI_S_VALUE), secretToken) };

        XDI3Statement[] targetStatementsSet = new XDI3Statement[] { XDI3Statement
                .fromLiteralComponents(XDI3Util.concatXris(
                        cloudNumber.getPeerRootXri(),
                        XDI3Segment.create("<$xdi><$uri>&")), cloudXdiEndpoint) };

        message.createOperation(XDI3Segment
                .create("$do<$digest><$secret><$token>"),
                Arrays.asList(targetStatementsDoDigestSecretToken).iterator());
        
        message.createSetOperation(Arrays.asList(targetStatementsSet)
                .iterator());

        // send message

        this.getXdiClientCSPRegistry().send(message.getMessageEnvelope(), null);

        // done

        log.debug("In CSP: Cloud registered with Cloud Number " + cloudNumber
                + " and Secret Token and Cloud XDI endpoint "
                + cloudXdiEndpoint);
    }

    /**
     * {@inheritDoc}
     */
    public CloudNumber checkCloudNameAvailableInRN(CloudName cloudName)
            throws Xdi2ClientException {

        CloudNumber cloudNumber;

        // prepare message to RN

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Segment targetAddress = cloudName.getPeerRootXri();

        message.createGetOperation(targetAddress);

        // send message

        MessageResult messageResult = this.getXdiClientRNRegistrationService()
                .send(message.getMessageEnvelope(), null);

        Relation relation = messageResult.getGraph().getDeepRelation(
                cloudName.getPeerRootXri(), XDIDictionaryConstants.XRI_S_REF);

        if (relation == null) {

            log.debug("In RN: Cloud Name " + cloudName + " is available");

            return null;
        }

        cloudNumber = CloudNumber.fromPeerRootXri(relation
                .getTargetContextNodeXri());

        // done

        log.debug("In RN: Cloud Name " + cloudName
                + " is already registered with Cloud Number " + cloudNumber);

        return cloudNumber;
    }

    /**
     * {@inheritDoc}
     */
    public CloudNumber registerCloudNameInRN(CloudName cloudName)
            throws Xdi2ClientException {

        // prepare message to RN

        MessageEnvelope messageEnvelope1 = new MessageEnvelope();

        Message message1 = messageEnvelope1.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message1.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message1.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message1.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Statement targetStatementSet1 = XDI3Statement
                .fromRelationComponents(cloudName.getPeerRootXri(),
                        XDIDictionaryConstants.XRI_S_REF,
                        XDIConstants.XRI_S_VARIABLE);

        message1.createSetOperation(targetStatementSet1);

        // send message 1 and read result

        MessageResult messageResult = this.getXdiClientRNRegistrationService()
                .send(message1.getMessageEnvelope(), null);

        Relation relation = messageResult.getGraph().getDeepRelation(
                cloudName.getPeerRootXri(), XDIDictionaryConstants.XRI_S_REF);
        if (relation == null)
            throw new RuntimeException("Cloud Number not registered.");

        CloudNumber cloudNumber = CloudNumber.fromPeerRootXri(relation
                .getTargetContextNodeXri());

        // prepare message 2 to RN

        MessageEnvelope messageEnvelope2 = new MessageEnvelope();

        Message message2 = messageEnvelope2.getMessageCollection(
                this.getCspInformation().getCspCloudNumber().getXri(), true)
                .createMessage(-1);
        message2.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message2.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message2.setSecretToken(this.getCspInformation().getCspSecretToken());

        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                this.getCspInformation(), cloudNumber);

        XDI3Statement targetStatementSet2 = XDI3Statement
                .fromLiteralComponents(XDI3Util.concatXris(
                        cloudNumber.getPeerRootXri(),
                        XDI3Segment.create("<$xdi><$uri>&")), cloudXdiEndpoint);

        message2.createSetOperation(targetStatementSet2);

        // send message 2

        this.getXdiClientRNRegistrationService().send(
                message2.getMessageEnvelope(), null);

        // done

        log.debug("In RN: Cloud Name " + cloudName
                + " registered with Cloud Number " + cloudNumber);

        return cloudNumber;
    }

    
    /**
     * {@inheritDoc}
     */
    public void registerCloudNameInRN(CloudName cloudName,
            CloudNumber cloudNumber) throws Xdi2ClientException {

        // prepare message 1 to RN

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message1 = messageEnvelope.getMessageCollection(
                this.getCspInformation().getCspCloudNumber().getXri(), true)
                .createMessage(-1);
        message1.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message1.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message1.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Statement targetStatementSet1 = XDI3Statement
                .fromRelationComponents(cloudName.getPeerRootXri(),
                        XDIDictionaryConstants.XRI_S_REF,
                        cloudNumber.getPeerRootXri());

        message1.createSetOperation(targetStatementSet1);

        // prepare message 2 to RN

        Message message2 = messageEnvelope.getMessageCollection(
                this.getCspInformation().getCspCloudNumber().getXri(), true)
                .createMessage(-1);
        message2.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message2.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message2.setSecretToken(this.getCspInformation().getCspSecretToken());

        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                this.getCspInformation(), cloudNumber);

        XDI3Statement targetStatementSet2 = XDI3Statement
                .fromLiteralComponents(XDI3Util.concatXris(
                        cloudNumber.getPeerRootXri(),
                        XDI3Segment.create("<$xdi><$uri>&")), cloudXdiEndpoint);

        message2.createSetOperation(targetStatementSet2);

        // send messages

        MessageResult messageResult = this.getXdiClientRNRegistrationService()
                .send(message1.getMessageEnvelope(), null);

        Relation relation = messageResult.getGraph().getDeepRelation(
                cloudName.getPeerRootXri(), XDIDictionaryConstants.XRI_S_REF);
        if (relation == null)
            throw new RuntimeException("Cloud Name not registered.");

        if (!cloudNumber.equals(CloudNumber.fromPeerRootXri(relation
                .getTargetContextNodeXri())))
            throw new RuntimeException("Registered Cloud Number "
                    + XdiPeerRoot.getXriOfPeerRootArcXri(relation
                            .getTargetContextNodeXri().getFirstSubSegment())
                    + " does not match requested Cloud Number " + cloudNumber);

        // done

        log.debug("In RN: Cloud Name " + cloudName
                + " registered with Cloud Number " + cloudNumber);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCloudNameInCSP(CloudName cloudName,
            CloudNumber cloudNumber) throws Xdi2ClientException {

        // prepare message to CSP

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getCspCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Statement[] targetStatementsSet = new XDI3Statement[] { XDI3Statement
                .fromRelationComponents(cloudName.getPeerRootXri(),
                        XDIDictionaryConstants.XRI_S_REF,
                        cloudNumber.getPeerRootXri()) };

        message.createSetOperation(Arrays.asList(targetStatementsSet)
                .iterator());

        // send message

        this.getXdiClientCSPRegistry().send(message.getMessageEnvelope(), null);

        // done

        log.debug("In CSP: Cloud Name " + cloudName
                + " registered with Cloud Number " + cloudNumber);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCloudNameInCloud(CloudName cloudName,
            CloudNumber cloudNumber, String secretToken)
            throws Xdi2ClientException {

        // prepare message to Cloud

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(cloudNumber.getXri());
        message.setToPeerRootXri(cloudNumber.getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(secretToken);

        XDI3Statement[] targetStatementsSet = new XDI3Statement[] {
                XDI3Statement.fromRelationComponents(
                        cloudName.getPeerRootXri(),
                        XDIDictionaryConstants.XRI_S_REF,
                        cloudNumber.getPeerRootXri()),
                XDI3Statement.fromRelationComponents(cloudName.getXri(),
                        XDIDictionaryConstants.XRI_S_REF, cloudNumber.getXri()),
                XDI3Statement
                        .fromRelationComponents(cloudNumber.getXri(),
                                XDIDictionaryConstants.XRI_S_IS_REF,
                                cloudName.getXri()),
                XDI3Statement.fromRelationComponents(
                        XDILinkContractConstants.XRI_S_PUBLIC_DO,
                        XDILinkContractConstants.XRI_S_GET,
                        XDI3Segment.create("(" + cloudNumber.getXri() + "/"
                                + XDIDictionaryConstants.XRI_S_IS_REF + "/"
                                + XDIConstants.XRI_S_VARIABLE + ")")) };

        message.createSetOperation(Arrays.asList(targetStatementsSet)
                .iterator());

        // send message

        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                this.getCspInformation(), cloudNumber);

        XDIClient xdiClientCloud = new XDIHttpClient(cloudXdiEndpoint);

        xdiClientCloud.send(message.getMessageEnvelope(), null);

        // done

        log.debug("In Cloud: Cloud Name " + cloudName
                + " registered with Cloud Number " + cloudNumber);
    }

    public boolean checkVerifiedContactInformationInRN(String email,
            String phone) throws Xdi2ClientException {

        throw new RuntimeException("Not implemented");
    }

    public void setVerifiedContactInformationInRN(CloudNumber cloudNumber,
            String email, String phone) throws Xdi2ClientException {

        throw new RuntimeException("Not implemented");
    }

    public void setCloudXdiEndpointInRN(CloudNumber cloudNumber,
            String cloudXdiEndpoint) throws Xdi2ClientException {

        // auto-generate XDI endpoint

        if (cloudXdiEndpoint == null) {

            cloudXdiEndpoint = makeCloudXdiEndpoint(this.getCspInformation(),
                    cloudNumber);
        }
        
        // prepare message to RN

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getRnCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(REGISTRAR_LINK_CONTRACT);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Statement targetStatementSet = XDI3Statement.fromLiteralComponents(
                XDI3Util.concatXris(cloudNumber.getPeerRootXri(),
                        XDI3Segment.create("<$xdi><$uri>&")), cloudXdiEndpoint);

        message.createSetOperation(targetStatementSet);

        // send message

        this.getXdiClientRNRegistrationService().send(
                message.getMessageEnvelope(), null);

        // done

        log.debug("In RN: Cloud XDI endpoint " + cloudXdiEndpoint
                + " set for Cloud Number " + cloudNumber);
    }

    public void setCloudXdiEndpointInCSP(CloudNumber cloudNumber,
            String cloudXdiEndpoint) throws Xdi2ClientException {

        // auto-generate XDI endpoint

        if (cloudXdiEndpoint == null) {

            cloudXdiEndpoint = makeCloudXdiEndpoint(this.getCspInformation(),
                    cloudNumber);
        }

        // prepare message to CSP

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getCspCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        cloudXdiEndpoint = makeCloudXdiEndpoint(this.getCspInformation(),
                cloudNumber);

        XDI3Statement targetStatementSet = XDI3Statement.fromLiteralComponents(
                XDI3Util.concatXris(cloudNumber.getPeerRootXri(),
                        XDI3Segment.create("<$xdi><$uri>&")), cloudXdiEndpoint);

        message.createSetOperation(targetStatementSet);

        // send message

        this.getXdiClientCSPRegistry().send(message.getMessageEnvelope(), null);

        // done

        log.debug("In CSP: Cloud XDI endpoint " + cloudXdiEndpoint
                + " set for Cloud Number " + cloudNumber);
    }

    public void setCloudSecretTokenInCSP(CloudNumber cloudNumber,
            String secretToken) throws Xdi2ClientException {

        // prepare message to CSP

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(this
                .getCspInformation().getCspCloudNumber().getXri());
        message.setToPeerRootXri(this.getCspInformation().getCspCloudNumber()
                .getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(this.getCspInformation().getCspSecretToken());

        XDI3Statement[] targetStatementsDoDigestSecretToken = new XDI3Statement[] { XDI3Statement
                .fromLiteralComponents(XDI3Util.concatXris(
                        cloudNumber.getPeerRootXri(),
                        XDIAuthenticationConstants.XRI_S_DIGEST_SECRET_TOKEN,
                        XDIConstants.XRI_S_VALUE), secretToken) };

        message.createOperation(XDI3Segment
                .create("$do<$digest><$secret><$token>"),
                Arrays.asList(targetStatementsDoDigestSecretToken).iterator());

        // send message

        this.getXdiClientCSPRegistry().send(message.getMessageEnvelope(), null);

        // done

        log.debug("In CSP: Secret token set for Cloud Number " + cloudNumber);
    }

    public void setCloudServicesInCloud(CloudNumber cloudNumber,
            String secretToken, Map<XDI3Segment, String> services)
            throws Xdi2ClientException {

        // prepare message to Cloud

        MessageEnvelope messageEnvelope = new MessageEnvelope();

        Message message = messageEnvelope.createMessage(cloudNumber.getXri());
        message.setToPeerRootXri(cloudNumber.getPeerRootXri());
        message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
        message.setSecretToken(secretToken);

        List<XDI3Statement> targetStatementsSet = new ArrayList<XDI3Statement>(
                services.size() * 2);

        for (Entry<XDI3Segment, String> entry : services.entrySet()) {

            targetStatementsSet.add(XDI3Statement.fromLiteralComponents(
                    XDI3Util.concatXris(entry.getKey(),
                            XDIClientConstants.XRI_S_URI,
                            XDIConstants.XRI_S_VALUE), entry.getValue()));

            targetStatementsSet.add(XDI3Statement.fromRelationComponents(
                    XDILinkContractConstants.XRI_S_PUBLIC_DO,
                    XDILinkContractConstants.XRI_S_GET, XDI3Util.concatXris(
                            entry.getKey(), XDIClientConstants.XRI_S_URI)));
        }

        message.createSetOperation(targetStatementsSet.iterator());

        // send message

        String cloudXdiEndpoint = makeCloudXdiEndpoint(
                this.getCspInformation(), cloudNumber);

        XDIClient xdiClientCloud = new XDIHttpClient(cloudXdiEndpoint);

        xdiClientCloud.send(message.getMessageEnvelope(), null);

        // done

        log.debug("In Cloud: For Cloud Number " + cloudNumber
                + " registered services " + services);
    }

    /*
     * Helper methods
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

    /*
     * Getters and setters
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public CSPInformation getCspInformation() {

        return this.cspInformation;
    }

    public void setCspInformation(CSPInformation cspInformation) {

        this.cspInformation = cspInformation;

        this.xdiClientCSPRegistry = new XDIHttpClient(
                cspInformation.getCspRegistryXdiEndpoint());
        this.xdiClientRNRegistrationService = new XDIHttpClient(
                cspInformation.getRnRegistrationServiceXdiEndpoint());
    }

    public XDIClient getXdiClientRNRegistrationService() {

        return this.xdiClientRNRegistrationService;
    }

    public void setXdiClientRNRegistrationService(
            XDIClient xdiClientRNRegistrationService) {

        this.xdiClientRNRegistrationService = xdiClientRNRegistrationService;
    }

    public XDIClient getXdiClientCSPRegistry() {

        return this.xdiClientCSPRegistry;
    }

    public void setXdiCSPEnvironmentRegistry(XDIClient xdiClientCSPRegistry) {

        this.xdiClientCSPRegistry = xdiClientCSPRegistry;
    }

    @Override
    public void registerUserCloud(CloudNumber cloudNumber, CloudName cloudName,
            String secretToken) throws Xdi2ClientException {
    }
    
    /**
     * @return the cloudNumberGenerator
     */
    public CloudNumberGenerator getCloudNumberGenerator() {
        return cloudNumberGenerator;
    }

    /**
     * @param cloudNumberGenerator the cloudNumberGenerator to set
     */
    public void setCloudNumberGenerator(CloudNumberGenerator cloudNumberGenerator) {
        this.cloudNumberGenerator = cloudNumberGenerator;
    }
    
    /**
     * @return the theNotifier
     */
    public Notifier getTheNotifier() {
        return theNotifier;
    }
    
    /**
     * @param theNotifier the theNotifier to set
     */
    public void setTheNotifier(Notifier theNotifier) {
        this.theNotifier = theNotifier;
    }

    /**
     * @return the messageManager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }


    /**
     * @param messageManager the messageManager to set
     */
    public void setMessageManager(MessageManager messageManager) {
        this.messageManager = messageManager;
    }


    /**
     * @return the tokenManager
     */
    public TokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * @param tokenManager the tokenManager to set
     */
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    /**
     * @return the validationEndPoint
     */
    public String getValidationEndpoint() {
        return validationEndpoint;
    }

    /**
     * @param validationEndpoint the validationEndPoint to set
     */
    public void setValidationEndpoint(String validationEndpoint) {
        this.validationEndpoint = validationEndpoint;
    }
    


 
    /**
     * {@inheritDoc}
     */
    @Override
    public CSPUserCredential signUpNewUser() throws CSPRegistrationException {

        log.debug("Signing Up New User");

        try {

            // 2.1.1.1
            CloudNumber cloudNumber = cloudNumberGenerator
                    .generateCloudNumber(XDIConstants.CS_EQUALS);
            
            // Create temporary Secret Token            
            String secretToken = RandomStringUtils.randomAlphanumeric(10).toUpperCase();
            
            log.debug("Creating CSP Graph with Cloud Number {}: Temp SecretToken {}", cloudNumber, secretToken );

            // 2.1.1.2: Create Default Graph in CSP ( applied temp token)
            registerCloudInCSP(cloudNumber, secretToken);
            
            CSPUserCredential tempCredential = new CSPUserCredential(cloudNumber, secretToken);

            // 2.1.1.4
            Map<XDI3Segment, String> cloudServices = new HashMap<XDI3Segment, String> ();

            //@TODO: Parameterize
            //Question about this syntax("<$xdi><$uri>&"))) };
            cloudServices.put(XDI3Segment.create("<$https><$connect><$xdi>"), "http://respectconnect-dev.respectnetwork.net/respectconnect/");
            setCloudServicesInCloud(cloudNumber, secretToken, cloudServices);

            return tempCredential;

        } catch (Xdi2ClientException e) {
            throw new CSPRegistrationException(e);
        } 
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void createAndValidateUser(CloudNumber cloudNumber, CSPUser theUser, String secretToken)
            throws CSPValidationException {
            
        try {
            
            if (validationEndpoint == null || tokenManager == null || theNotifier == null ) {
                throw new CSPValidationException("Basic CSP not properly configured,"
                        + " check that all required properties are set.");
            }
            
            String emailValidationCode = tokenManager.createToken(new TokenKey(cloudNumber.toString(), "EMAIL"));
            String smsValidationCode = tokenManager.createToken(new TokenKey(cloudNumber.toString(), "SMS"));
                 
            String emailMessage = messageManager.createEmailMessage(emailValidationCode, validationEndpoint, cloudNumber.toString());
            String smsMessage = messageManager.createSMSMessage(smsValidationCode );
                   
            theNotifier.sendEmailNotification(theUser.getEmail(), emailMessage);
            theNotifier.sendSMSNotification(theUser.getPhone(), smsMessage);
                    
            MessageEnvelope messageEnvelope = new MessageEnvelope();
            //Sender
            Message message = messageEnvelope.createMessage(cloudNumber.getXri());
            //TO
            message.setToPeerRootXri(cloudNumber.getPeerRootXri());
            message.setLinkContractXri(XDILinkContractConstants.XRI_S_DO);
            message.setSecretToken(secretToken);
               
    
            // Create User Graph Statements
    
            XDI3Statement[] targetStatementsSet = new XDI3Statement[8];
            targetStatementsSet[0] = XDI3Statement.create(cloudNumber.toString() + "<+name><+full>&/&/\"" + theUser.getName() + "\"");
            targetStatementsSet[1] = XDI3Statement.create(cloudNumber.toString() + "<+name><+nick>&/&/\"" + theUser.getNickName() + "\"");
    
            targetStatementsSet[2] = XDI3Statement.create(cloudNumber.toString() + "<+email>&/&/\"" + theUser.getEmail() + "\"");
            targetStatementsSet[3] = XDI3Statement.create(cloudNumber.toString() + "<+phone>&/&/\"" + theUser.getPhone() + "\"");
          
            targetStatementsSet[4] = XDI3Statement.create(cloudNumber.toString() + "<+addr><+street>&/&/\"" + theUser.getStreet()  + "\"");
            targetStatementsSet[5] = XDI3Statement.create(cloudNumber.toString() + "<+addr><+city>&/&/\"" + theUser.getCity()  + "\"");
            targetStatementsSet[6] = XDI3Statement.create(cloudNumber.toString() + "<+addr><+state>&/&/\"" + theUser.getState()  + "\"");
            targetStatementsSet[7] = XDI3Statement.create(cloudNumber.toString() + "<+addr><+postalcode>&/&/\"" + theUser.getPostalcode()  + "\"");
    
                    
            message.createSetOperation(Arrays.asList(targetStatementsSet)
                    .iterator());
            
            // send message
    
            String cloudXdiEndpoint = makeCloudXdiEndpoint(
                    this.getCspInformation(), cloudNumber);
    
            XDIClient xdiClientCloud = new XDIHttpClient(cloudXdiEndpoint);
    
            xdiClientCloud.send(message.getMessageEnvelope(), null);
    
            // done
    
            log.debug("Created User Graph with {}", theUser.toString());

        } catch (NotificationException e) {
            log.warn("Problem Notifying User: {}", e.getMessage());
            throw new CSPValidationException(e.getMessage());            
        } catch (TokenException e) {
            log.warn("Problem Generating Verification Token for User: {}", e.getMessage());
            throw new CSPValidationException(e.getMessage());        
        } catch (MessageCreationException e) {
            log.warn("Problem Creating Notification Message: {}", e.getMessage());
            throw new CSPValidationException(e.getMessage());
        } catch (Xdi2ClientException e) {
            log.warn("Problem Creating User Graph Data", e.getMessage());
            throw new CSPValidationException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateCodes(CloudNumber cloudNumber, String emailCode,
            String smsCode) throws CSPValidationException {
        
        try {
            boolean result = (tokenManager.validateToken(new TokenKey(cloudNumber.toString(), "EMAIL"), emailCode) &&
                tokenManager.validateToken(new TokenKey(cloudNumber.toString(), "SMS"), smsCode));
            
            //If the codes are used for verification once  they should then be invalidated.
            if (result){
                tokenManager.inValidateToken(new TokenKey(cloudNumber.toString(), "EMAIL"));
                tokenManager.inValidateToken(new TokenKey(cloudNumber.toString(), "SMS"));
            }
           
            return result;
            
        } catch (TokenException e) {
            String error = "Error validating token: {}" + e.getMessage();
            log.debug(error);
            throw new CSPValidationException(error);
        }
    }
    

}
