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

public class ValidateMarketingCopyLifecyclePhasePostPX implements IEventAction
{
    private static final Logger LOGGER        = Logger.getLogger(ValidateMarketingCopyLifecyclePhasePostPX.class.getName());
    private StringBuilder       bannerMessage = new StringBuilder();

    ValidateMarketCopyAndColorBoxLifecyclePhase validatePX = new ValidateMarketCopyAndColorBoxLifecyclePhase();
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
           // String graphicReqType = "";
            final ITable aiTable = grco.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
            final Iterator<IRow> iterator = aiTable.iterator();
            while (iterator.hasNext())
            {
                final IRow row = iterator.next();
                final IItem grObject = (IItem) row.getReferent();
                if (row.getValue(ChangeConstants.ATT_AFFECTED_ITEMS_ITEM_TYPE).toString().equalsIgnoreCase(PropertiesLoader.getProperty("SUBCLASS_GRAPHIC_REQUISITION")))
                {
                    /*graphicReqType = validatePX.getAttributeValueFromDataObj(grObject,
                                                                  ItemConstants.TABLE_REDLINEPAGETHREE,
                                                                  grco,
                                                                  Constants.GR_REQUEST_TYPE,
                                                                  true);
                    if (!Constants.ALLOWED_GR_REQUEST_TYPE.contains(graphicReqType))
                    {*/
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
                validatePX.sendNotificationToOriginator(bean, grco);
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
     * Validates and update Marketing Copy Lifecyclsphase
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

            isLifecyclePhaseInProduction = validatePX.isLifecyclePhaseinProduction(grChildObject);

            // Check if item type is MarketingCopy
            if (itemType.equals(Constants.SUBCLASS_MARKETINGCOPY))
            {
                if (!isLifecyclePhaseInProduction)
                {
                    isMarketingChangeRequired = true;
                    grChildList.add(grChildObject);
                }
            }
            
        }
        if (isMarketingChangeRequired)
        {
            // Create Marketing Change Order
            if (grObject != null)
            {
                marketingChange = validatePX.createMarketingChange(session, grObject.toString());
                if (marketingChange != null)
                {
                    // Put marketing copy in Change and set required values
                    validatePX.addChildObjectInMarketingChange(marketingChange, grChildList, session);
                    // Release the Change
                    validatePX.releaseMarketingChange(marketingChange, session);

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
 }
