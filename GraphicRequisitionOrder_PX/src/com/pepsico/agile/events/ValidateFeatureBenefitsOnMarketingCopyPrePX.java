package com.pepsico.agile.events;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.agile.api.UserConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventActionResult;
import com.agile.px.IEventAction;
import com.agile.px.IEventInfo;
import com.agile.px.IWFChangeStatusEventInfo;
import com.pepsico.agile.events.beans.NotificationBean;
import com.pepsico.agile.px.util.PropertiesLoader;

public class ValidateFeatureBenefitsOnMarketingCopyPrePX implements IEventAction {
	private static final Logger LOGGER = Logger
			.getLogger(ValidateFeatureBenefitsOnMarketingCopyPrePX.class.getName());
	private StringBuilder bannerMessage = new StringBuilder();
	private int count=0;

	public EventActionResult doAction(IAgileSession agileSession, INode node, IEventInfo eventInfo) {
		ActionResult actionResult = null;
		try {
			final NotificationBean bean = new NotificationBean();
			PropertiesLoader.loadResource("CornerStone");
			DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
			LOGGER.info("Start validation to check the  feature benefits on marketing copy with label specification.");
			final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
			final IChange grco = (IChange) changeStatusEvent.getDataObject();
			final ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			final Iterator<IRow> iterator = aiTable.iterator();
			String graphicReqType = "";
			List<String> claimMadeListVal= null;
			List<String> grTypeList= null;
			while (iterator.hasNext()) {
				final IRow row = iterator.next();
				final IItem grObject = (IItem) row.getReferent();
				if (row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString()
						.equalsIgnoreCase(PropertiesLoader.getProperty("SUBCLASS_GRAPHIC_REQUISITION"))) {
					graphicReqType = getAttributeValueFromDataObj(grObject, ItemConstants.TABLE_REDLINEPAGETHREE, grco,
							Constants.GR_REQUEST_TYPE, true);
					grTypeList	= Arrays.asList(Constants.ALLOWED_GR_REQUEST_TYPE.split(";"));
					if (!grTypeList.contains(graphicReqType)) {
						LOGGER.info("GR to be validated = " + grObject);
						claimMadeListVal = getLabelCountAnsClaimMade(grObject, grco);
						if(count==1){
							bannerMessage.append(getFeatureBenefitsValueFromMarketingCopy(grObject,
									ItemConstants.TABLE_REDLINEBOM, grco, claimMadeListVal));
						}
						
					} else {
						LOGGER.info(
								"GR Feature Benefits Validation not applicable for GR Request Type" + graphicReqType);
					}
				}
			}
			if (bannerMessage.toString().isEmpty()) {
				actionResult = new ActionResult(ActionResult.STRING, Constants.SUCCESS_MESSAGE);
			} else {				
				actionResult = new ActionResult(ActionResult.EXCEPTION, new Exception(bannerMessage.toString()));
				bean.setNotificationSubject(Constants.NOTIFICATIONBODY_FEATUREBENEFIT_MISMATCH);
				bean.setNotificationBody("Validation Failed: "+bannerMessage.toString());                
                sendNotificationToOriginator(bean, grco);
			}
			LOGGER.info(
					"End of the Change Status Event - ValidateFeatureBenefitsOnMarketingCopyPrePX");

		} catch (APIException api) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, api);
			LOGGER.error("API Error : ", api);
		} catch (Exception ex) {
			actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
			LOGGER.error("Error : ", ex);
		}
		return new EventActionResult(eventInfo, actionResult);
	}

	/**
	 * Validate Feature Benefits value from Label Spec's Claim being Made (In
	 * Primary Section)
	 * 
	 * @param grObject
	 * @param tableRedlinebom
	 * @param grco
	 * @param claimMade
	 * @return
	 * @throws APIException
	 */
	private String getFeatureBenefitsValueFromMarketingCopy(IItem grObject, Integer tableRedlinebom, IChange grco,
			List<String> claimMade) throws APIException {
		grObject.setRevision(grco);
		String message="";
		IItem mcObj = null;
		final Iterator<IRow> redlineBomItr = grObject.getTable(tableRedlinebom).iterator();
		while (redlineBomItr.hasNext()) {
			IRow bomRow = (IRow) redlineBomItr.next();
			boolean isBomItemDeleted = bomRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED);
			if(!isBomItemDeleted){
			String bomType = bomRow.getValue(ItemConstants.ATT_BOM_ITEM_TYPE).toString();
			if (bomType.equals(PropertiesLoader.getProperty("SUBCLASS_MARKETING_COPY"))) {
				mcObj = (IItem) bomRow.getReferent();
				LOGGER.info("mcObj:::::"+mcObj);
				if (mcObj != null) {
					String fb1 = mcObj
							.getCell(getBaseIdOrAPIName(PropertiesLoader.getProperty("FEATURE_BENEFIT_1"))).getValue().toString();
					String featureBenefit1 = fb1.substring(fb1.indexOf("-") + 1, fb1.length()).toLowerCase().trim();
							
					LOGGER.info("featureBenefit1::"+featureBenefit1);
					String fb2 =mcObj
							.getCell(getBaseIdOrAPIName(PropertiesLoader.getProperty("FEATURE_BENEFIT_2"))).getValue().toString(); 
					String featureBenefit2 = fb2.substring(fb2.indexOf("-") + 1, fb2.length()).toLowerCase().trim();
					
					LOGGER.info("featureBenefit2::"+featureBenefit2);
					String fb3 =mcObj
							.getCell(getBaseIdOrAPIName(PropertiesLoader.getProperty("FEATURE_BENEFIT_3"))).getValue().toString();
					String featureBenefit3 = fb3.substring(fb3.indexOf("-") + 1, fb3.length()).toLowerCase().trim();
					
					LOGGER.info("featureBenefit3::"+featureBenefit3);
					String fb4= mcObj
							.getCell(getBaseIdOrAPIName(PropertiesLoader.getProperty("FEATURE_BENEFIT_4"))).getValue().toString();
					String featureBenefit4 = fb4.substring(fb4.indexOf("-") + 1, fb4.length()).toLowerCase().trim();
					
					LOGGER.info("featureBenefit4::"+featureBenefit4);
					
					if (featureBenefit1!=null && !featureBenefit1.equals("") && (claimMade==null || claimMade.equals(""))) {
						LOGGER.info("Condition 1");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb1 });
						break;
					}
					if (featureBenefit2!=null && !featureBenefit2.equals("") && (claimMade==null || claimMade.equals(""))) {
						LOGGER.info("Condition 2");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb2 });
						break;
					}
					if (featureBenefit3!=null && !featureBenefit3.equals("") && (claimMade==null || claimMade.equals(""))) {
						LOGGER.info("Condition 3");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb3 });
						break;
					}
					if (featureBenefit4!=null && !featureBenefit4.equals("") && (claimMade==null || claimMade.equals(""))) {
						LOGGER.info("Condition 4");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb4 });
						break;
					}
					if (featureBenefit1!=null && !featureBenefit1.equals("") && !claimMade.contains(featureBenefit1)) {
						LOGGER.info("Condition 5");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb1 });
						break;
					} else if (featureBenefit2!=null && !featureBenefit2.equals("") && !claimMade.contains(featureBenefit2)) {
						LOGGER.info("Condition 6");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb2 });
						break;
					} else if (featureBenefit3!=null && !featureBenefit3.equals("") &&!claimMade.contains(featureBenefit3)) {
						LOGGER.info("Condition 7");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb3 });
						break;
					} else if (featureBenefit4!=null && !featureBenefit4.equals("") && !claimMade.contains(featureBenefit4)) {
						LOGGER.info("Condition 8");
						message=
								MessageFormat.format(PropertiesLoader.getProperty("GR_FEATURE_BENEFIT_MISMATCH_ERROR"),
										new Object[] { grco.getName(), fb4 });
						break;
					}
				}
			}
		}
		}
		return message;
	}

	/**
	 * Bypass GR validation if  label spec count is >1
	 * 
	 * @param grObject
	 * @param grco
	 * @return
	 * @throws APIException
	 */
	private List<String>  getLabelCountAnsClaimMade(IItem grObject, IChange grco) throws APIException {
		List<String>  returnMessage = null;
		String itemNumber = "";
		IItem labelObj = null;
		final Set<String> labelSpecCount = new HashSet<String>();
		grObject.setRevision(grco);
		if (grObject != null) {
			final ITable redlineTable = grObject.getTable(ItemConstants.TABLE_REDLINEBOM);
			final Iterator<IRow> bomIterator = redlineTable.iterator();
			while (bomIterator.hasNext()) {
				IRow bomRow = bomIterator.next();
				boolean isBomItemDeleted = bomRow.isFlagSet(ItemConstants.FLAG_IS_REDLINE_REMOVED);
				if(!isBomItemDeleted){
				itemNumber = bomRow.getValue(ItemConstants.ATT_BOM_ITEM_NUMBER).toString();
				LOGGER.info("ItemNumber:::"+itemNumber);
				if (bomRow.getValue(ItemConstants.ATT_BOM_ITEM_TYPE).toString()
						.equals(PropertiesLoader.getProperty("SUBCLASS_LABEL_SPECIFICATION"))) {
					labelObj = (IItem) bomRow.getReferent();
					labelSpecCount.add(itemNumber);
				}
				}
			}
			LOGGER.info("Label Spec Count :::"+labelSpecCount.size());
			if (labelSpecCount.size() == 1) {
				returnMessage = getClaimBeingMadeValue(labelObj);
				count=1;
			} else if (labelSpecCount.size() > 1) {
				LOGGER.info("More Than 1 Label Spec associated to GR.");
				count =labelSpecCount.size();
				returnMessage =null;
			}
		}
		return returnMessage;
	}

	/**
	 * Get Claims Being Made (In Primary Language) from Label Specification
	 * 
	 * @param labelObj
	 * @return
	 * @throws APIException
	 */
	private List<String> getClaimBeingMadeValue(IItem labelObj) throws APIException {
		String claimsBeingMade = labelObj.getCell(Constants.BASEID_CLAIMSBEINGMADE).toString().toLowerCase();
		String[] splitVal= null;
		List<String> listVal=null;
		if(claimsBeingMade!=null && !claimsBeingMade.equals("")){
			splitVal= claimsBeingMade.split(",");
			for (int i = 0; i < splitVal.length; i++){
				splitVal[i] = splitVal[i].trim();
			}
			listVal = Arrays.asList(splitVal);
		}
		LOGGER.info("Claim Made listVal"+listVal);
		return listVal;

	}

	/**
	 * Get required attribute associated with GR. If GR has pending change then
	 * get it from inputed table table else get it directly
	 * 
	 * @param dataObj
	 * @param attrTableID
	 * @param changeObj
	 * @param attrName
	 * @param isChangeControlled
	 * @return
	 * @throws APIException
	 */
	@SuppressWarnings("unchecked")
	private String getAttributeValueFromDataObj(final IItem dataObj, final Integer attrTableID, final IChange changeObj,
			final String attrName, final boolean isChangeControlled) throws APIException {
		String attrValue = "";
		// If change controlled get from redline table
		dataObj.setRevision(changeObj);
		if (isChangeControlled) {
			final Iterator<IRow> p3Attrs = dataObj.getTable(attrTableID).iterator();
			IRow p3Row = p3Attrs.next();
			attrValue = p3Row.getValue(getBaseIdOrAPIName(attrName)).toString();
		} else {
			// Else get directly
			attrValue = dataObj.getValue(getBaseIdOrAPIName(attrName)).toString();
		}
		return attrValue;
	}

	/**
	 * Check if the input is String or integer and return likewise
	 * 
	 * @param input
	 * @return Object
	 */
	private Object getBaseIdOrAPIName(final String input) {
		Object output = null;
		final String regex = "[0-9]+";
		if (input.matches(regex)) {
			output = Integer.parseInt(input);
		} else {
			output = input;
		}
		return output;
	}

	 /**
     * Send notification to the grco originator if pre validation fails.
     * 
     * @param bean
     * @param bpco
     * @throws MessagingException
     * @throws APIException
     */
    public void sendNotificationToOriginator(final NotificationBean bean,
                                             final IChange grco)
        throws MessagingException, APIException
    {
        if (!bean.getNotificationBody().isEmpty())
        {
            final IUser currentUser = (IUser) grco.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).getReferent();
            final String emailAddress = currentUser.getValue(UserConstants.ATT_GENERAL_INFO_EMAIL).toString();
            final Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", PropertiesLoader.getProperty("Mail.Hostname"));
            final Session session = Session.getDefaultInstance(properties);
            final MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(PropertiesLoader.getProperty("Mail.FromAddress")));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
            message.setSubject(bean.getNotificationSubject());
            final StringBuilder content = new StringBuilder();
            content.append(bean.getNotificationBody());
            message.setContent(content.toString(), "text/html");
            Transport.send(message);
        }
    }
}
