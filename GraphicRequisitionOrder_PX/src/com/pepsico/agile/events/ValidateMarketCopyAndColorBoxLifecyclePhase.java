package com.pepsico.agile.events;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAdmin;
import com.agile.api.IAgileClass;
import com.agile.api.IAgileSession;
import com.agile.api.IAutoNumber;
import com.agile.api.IChange;
import com.agile.api.IItem;
import com.agile.api.INode;
import com.agile.api.IRow;
import com.agile.api.IStatus;
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

public class ValidateMarketCopyAndColorBoxLifecyclePhase implements IEventAction
{
    private static final Logger LOGGER        = Logger.getLogger(ValidateMarketCopyAndColorBoxLifecyclePhase.class.getName());
    private StringBuilder       bannerMessage = new StringBuilder();


    @Override
    public EventActionResult doAction(IAgileSession session,
                                      INode arg1,
                                      IEventInfo eventInfo)
    {
        ActionResult actionResult = null;
        //IChange grco = null;
        try
        {
            final NotificationBean bean = new NotificationBean();
            PropertiesLoader.loadResource("CornerStone");
            DOMConfigurator.configure(PropertiesLoader.getProperty("LOG4J_CONFIG"));
            LOGGER.info("******************Start  validation of Marketing copy and color Box Lifecycle Phase*******************");
            final StringBuilder builder = new StringBuilder();
            final IWFChangeStatusEventInfo changeStatusEvent = (IWFChangeStatusEventInfo) eventInfo;
            final IChange grco = (IChange) changeStatusEvent.getDataObject();
            //String graphicReqType = "";
            final ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
            final Iterator<IRow> iterator = aiTable.iterator();
            while (iterator.hasNext())
            {
                final IRow row = iterator.next();
                final IItem grObject = (IItem) row.getReferent();
                if (row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString().equalsIgnoreCase(PropertiesLoader.getProperty("SUBCLASS_GRAPHIC_REQUISITION")))
                {
                   /* graphicReqType = getAttributeValueFromDataObj(grObject,
                                                                  ItemConstants.TABLE_REDLINEPAGETHREE,
                                                                  grco,
                                                                  Constants.GR_REQUEST_TYPE,
                                                                  true);*/
                   // if (!Constants.ALLOWED_GR_REQUEST_TYPE.contains(graphicReqType))
                   // {
                        LOGGER.info("GR to be validated = " + grObject);

                        bannerMessage.append(ValidateAndUpdateMarketingCopyAndColorBoxLifecyclePhase(grco,
                                                                                                     grObject,
                                                                                                     session));

                   /* }
                    else
                    {
                        LOGGER.info("GR Feature Benefits Validation not applicable for GR Request Type" + graphicReqType);
                    }*/
                }
            }
            if (bannerMessage.toString().isEmpty())
            {
                actionResult = new ActionResult(ActionResult.STRING, Constants.SUCCESS_MESSAGE);
            }
            else
            {
                actionResult = new ActionResult(ActionResult.EXCEPTION,
                                                new Exception(bannerMessage.toString()));
                bean.setNotificationSubject(PropertiesLoader.getProperty("MAIL_SUBJECT_EXCEPTION"));
                bean.setNotificationBody("Validation Failed: " + bannerMessage.toString());
                sendNotificationToOriginator(bean, grco);
            }
            LOGGER.info("*************************End of validation of Marketing copy and color Box Lifecycle Phase**************");

        }
        catch (APIException api)
        {
            actionResult = new ActionResult(ActionResult.EXCEPTION, api);
            LOGGER.error("API Error : ", api);
        }
        catch (Exception ex)
        {
            actionResult = new ActionResult(ActionResult.EXCEPTION, ex);
            LOGGER.error("Error : ", ex);
        }
        return new EventActionResult(eventInfo, actionResult);

    }


    /**
     * Validates and update Marketing Copy and Color Box Lifecyclsphase
     * 
     * @param grco
     * @param grObject
     * @param session
     * @return
     * @throws APIException
     */
    public String ValidateAndUpdateMarketingCopyAndColorBoxLifecyclePhase(IChange grco,
                                                                          IItem grObject,
                                                                          IAgileSession session)
        throws APIException
    {
        String message = "";
        boolean isMarketingChangeRequired = false;
        IChange marketingChange = null;
        IItem grChildObject = null;
        boolean isLifecyclePhaseInProduction = false;
        Set<IItem> grChildList = new HashSet<IItem>();
        ITable bomTable = grObject.getTable(ItemConstants.TABLE_BOM);
        Iterator<?> bomItr = bomTable.iterator();
        while (bomItr.hasNext())
        {
            IRow bomRow = (IRow) bomItr.next();
            grChildObject = (IItem) bomRow.getReferent();
            String itemType = bomRow.getCell(ItemConstants.ATT_BOM_ITEM_TYPE).toString();

            isLifecyclePhaseInProduction = isLifecyclePhaseinProduction(grChildObject);

            // Check if item type is Color/PMS/FL#/Type
            if (itemType.equals(Constants.SUBCLASS_COLOR_BOX))
            {

                String usageType = grChildObject.getValue(ItemConstants.ATT_PAGE_TWO_LIST40).toString();
                LOGGER.debug("Usage type" + usageType);

                if (usageType != null && usageType.equals(Constants.USAGETYPE_PARENT))
                {
                    if (!isLifecyclePhaseInProduction)
                    {
                        isMarketingChangeRequired = true;
                        grChildList.add(grChildObject);
                    }
                    ITable colorBoxBomTab = grChildObject.getTable(ItemConstants.TABLE_BOM);
                    Iterator<?> it = colorBoxBomTab.iterator();
                    while (it.hasNext())
                    {
                        IRow colorBoxBomRow = (IRow) it.next();
                        IItem childBoxObject = (IItem) colorBoxBomRow.getReferent();
                        isLifecyclePhaseInProduction = isLifecyclePhaseinProduction(childBoxObject);
                        if (!isLifecyclePhaseInProduction)
                        {
                            isMarketingChangeRequired = true;
                            grChildList.add(childBoxObject);
                        }
                    }
                }
                else if (usageType != null && usageType.equals(Constants.USAGETYPE_CHILD))
                {
                    LOGGER.error(Constants.USAGETYPE_CHILD);
                    // System.out.println("“GRCO-XXXXXXXXXX has failed to auto promote because Color/PMS/FL#/Type has failed to auto promote to Production status because Colo/PMS/FL#/Type Parent is not present in the GR-XXXXX BOM Tab.” ");
                    message = MessageFormat.format(PropertiesLoader.getProperty("ERROR_MSG_USAGETYPE_IS_CHILD"),
                                                   new Object[] {
                                                           grco.toString(),
                                                           grObject.toString()
                                                   });
                    return message;

                }
            }
        }
        if (isMarketingChangeRequired)
        {
            // Create Marketing Change Order
            if (grObject != null)
            {
                marketingChange = createMarketingChange(session, grObject.toString());
                if (marketingChange != null)
                {
                    // Put marketing copy in Change and set required values
                    addChildObjectInMarketingChange(marketingChange, grChildList, session);
                    // Release the Change
                    releaseMarketingChange(marketingChange, session);

                }
                else
                {
                    LOGGER.error(Constants.MARKETINGCO_NOTCREATED);
                    message = MessageFormat.format(PropertiesLoader.getProperty("MARKETINGCO_NOTCREATED_ERROR_MSG"),
                                                   new Object[] {
                                                           grco.toString(),
                                                           grObject.toString()
                                                   });
                    return message;
                }
            }
        }

        return message;
    }


    /**
     * Check if lifecycle phase is inProduction
     * 
     * @param gRChildObject
     * @return
     * @throws APIException
     */
    public boolean isLifecyclePhaseinProduction(IItem gRChildObject)
        throws APIException
    {
        boolean isLifecyclePhase = false;
        if (gRChildObject != null && !gRChildObject.equals(""))
        {
            String lifecyclePhase = gRChildObject.getValue(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE).toString();
            if (lifecyclePhase.equalsIgnoreCase(Constants.LIFECYCLEPHASE_PRODUCTION))
            {
                isLifecyclePhase = true;
            }
        }
        return isLifecyclePhase;
    }


    /**
     * Add objects to Marketing CO
     * 
     * @param marketingChange
     * @param gRChildList
     * @param session
     * @throws APIException
     */
    public void addChildObjectInMarketingChange(IChange marketingChange,
                                                        Set<IItem> gRChildList,
                                                        IAgileSession session)
        throws APIException
    {
        ITable affectedItemsTable = marketingChange.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
        Iterator<?> iter = gRChildList.iterator();
        while (iter.hasNext())
        {
            session.disableAllWarnings();
            affectedItemsTable.createRow(iter.next());
            Iterator<?> affectedItr = affectedItemsTable.iterator();
            while (affectedItr.hasNext())
            {
                IRow row = (IRow) affectedItr.next();
                row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).setValue(Constants.NEW_REV);
                row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).setValue(Constants.LIFECYCLEPHASE_PRODUCTION);
            }
        }
    }


    /**
     * Release Marketing CO
     * 
     * @param marketingChange
     * @param session
     * @throws APIException
     */
    public void releaseMarketingChange(IChange marketingChange,
                                               IAgileSession session)
        throws APIException
    {
        session.disableAllWarnings();
        IStatus nextStatus = marketingChange.getDefaultNextStatus();
        marketingChange.changeStatus(nextStatus, false, "", false, false, null, null, null, false);
    }


    public IChange createMarketingChange(IAgileSession sourceSession,
                                                 String gRObject)
        throws APIException
    {
        // Create a Change object
        Map<Integer, Object> params = new HashMap<Integer, Object>();
        IChange marketingCopyChange = null;
        IAdmin admin = sourceSession.getAdminInstance();
        Integer processId = null;
        IAgileClass[] cls = admin.getAgileClasses(IAdmin.CONCRETE);
        for (int i = 0; i < cls.length; i++)
        {
            if (cls[i].getName().equals(Constants.SUBCLASS_MARKETING_CHANGE_ORDER))
            {
                processId = (Integer) cls[i].getId();
                break;
            }
        }
        IAgileClass docCls = admin.getAgileClass(processId);
        IAutoNumber[] numSources = docCls.getAutoNumberSources();
        String nextAvailableAutoNumber = numSources[0].getNextNumber(docCls);

        params.put(ChangeConstants.ATT_COVER_PAGE_NUMBER, nextAvailableAutoNumber);
        params.put(ChangeConstants.ATT_COVER_PAGE_DESCRIPTION_OF_CHANGE,
                   "Marketing CO automatically created and released as part of autorelease of Marketing Copy/Color Box for '" + gRObject +
                       "' ");
        params.put(ChangeConstants.ATT_COVER_PAGE_WORKFLOW, Constants.WORKFLOW_MARKETING);

        marketingCopyChange = (IChange) sourceSession.createObject(processId, params);
        return marketingCopyChange;
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


    /**
     * @param dataObj
     * @param attrTableID
     * @param changeObj
     * @param attrName
     * @param isChangeControlled
     * @return
     * @throws APIException
     */
    public String getAttributeValueFromDataObj(final IItem dataObj,
                                                final Integer attrTableID,
                                                final IChange changeObj,
                                                final String attrName,
                                                final boolean isChangeControlled)
        throws APIException
    {
        String attrValue = "";
        // If change controlled get from redline table
        dataObj.setRevision(changeObj);
        if (isChangeControlled)
        {
            final Iterator<IRow> p3Attrs = dataObj.getTable(attrTableID).iterator();
            IRow p3Row = p3Attrs.next();
            attrValue = p3Row.getValue(getBaseIdOrAPIName(attrName)).toString();
        }
        else
        {
            // Else get directly
            attrValue = dataObj.getValue(getBaseIdOrAPIName(attrName)).toString();
        }
        return attrValue;
    }


    /**
     * @param input
     * @return
     */
    public Object getBaseIdOrAPIName(final String input)
    {
        Object output = null;
        final String regex = "[0-9]+";
        if (input.matches(regex))
        {
            output = Integer.parseInt(input);
        }
        else
        {
            output = input;
        }
        return output;
    }

}
